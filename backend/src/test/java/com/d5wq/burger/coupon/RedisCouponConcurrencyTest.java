package com.d5wq.burger.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import com.d5wq.burger.coupon.dto.CouponCreateRequest;
import com.d5wq.burger.coupon.dto.CouponStatusResponse;
import com.d5wq.burger.coupon.service.CouponIssueService;
import com.d5wq.burger.coupon.service.IssueStrategy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Step 5: Redis(Redisson) 원자연산 발급이 동일 부하에서 <b>정확히 재고만큼만</b> 발급함을 검증한다.
 * (naive 968 → 비관적 락 100 → redis 100 — 결과는 같지만 경쟁 지점을 DB 행 락이 아니라 Redis 가 처리한다)
 *
 * <p>테스트 시작 시 임시 {@code redis-server} 프로세스를 띄우고 Redisson 을 그쪽에 연결한다.
 * 실행 환경에 {@code redis-server} 가 없으면 테스트를 건너뛴다(assumeTrue).
 */
@ActiveProfiles("redis-test")
@SpringBootTest
class RedisCouponConcurrencyTest {

    private static final int STOCK = 100;
    private static final int USERS = 1000;
    private static final int THREADS = 50;

    private static Process redisProcess;
    private static int redisPort;
    private static boolean redisReady;

    // 클래스 로드 시점(컨텍스트 로드보다 먼저) 임시 redis-server 를 띄운다.
    static {
        try {
            redisPort = findFreePort();
            redisProcess = new ProcessBuilder(
                    "redis-server", "--port", String.valueOf(redisPort),
                    "--save", "", "--appendonly", "no")
                    .redirectErrorStream(true)
                    .start();
            redisReady = waitForPort("127.0.0.1", redisPort, 5000);
        } catch (IOException | RuntimeException e) {
            redisReady = false;
        }
    }

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> redisPort);
    }

    @BeforeAll
    static void requireRedis() {
        Assumptions.assumeTrue(redisReady, "redis-server 를 띄울 수 없어 Redis 동시성 테스트를 건너뜁니다");
    }

    @AfterAll
    static void stopRedis() {
        if (redisProcess != null) {
            redisProcess.destroy();
        }
    }

    @Autowired
    CouponIssueService couponIssueService;

    @Test
    @DisplayName("Redis 원자연산: 재고 100 + 동시 1000요청 → 정확히 100장만 발급, 초과 0")
    void redis_noOverIssue_underConcurrency() throws InterruptedException {
        Long couponId = couponIssueService.createCoupon(
                new CouponCreateRequest("동시성 테스트 쿠폰(redis)", 20, null, null, STOCK, null, null));
        couponIssueService.reset(couponId); // DB 재고 + Redis 카운터/집합 시드

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(USERS);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        for (int i = 0; i < USERS; i++) {
            long userId = i + 1;
            pool.submit(() -> {
                try {
                    startGate.await();
                    couponIssueService.issue(couponId, userId, IssueStrategy.REDIS);
                    success.incrementAndGet();
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("소진")) {
                        soldOut.incrementAndGet();
                    } else {
                        other.incrementAndGet();
                    }
                } finally {
                    done.countDown();
                }
            });
        }
        startGate.countDown();
        done.await();
        pool.shutdown();

        CouponStatusResponse status = couponIssueService.getStatus(couponId);
        System.out.println("========== [Redis 원자연산 결과] ==========");
        System.out.println(" 총 수량(totalQuantity) : " + status.totalQuantity());
        System.out.println(" 남은 재고(stock)        : " + status.stock());
        System.out.println(" 발급 성공 응답 수        : " + success.get());
        System.out.println(" 품절 응답 수             : " + soldOut.get());
        System.out.println(" 기타 실패 수             : " + other.get());
        System.out.println(" 실제 발급 내역 행 수     : " + status.issuedRows() + "  (기대: " + STOCK + ")");
        System.out.println(" 초과 발급               : " + (status.issuedRows() - STOCK) + " 장");
        System.out.println("==========================================");

        assertThat(status.issuedRows())
                .as("Redis 원자연산은 정확히 재고만큼만 발급한다")
                .isEqualTo(STOCK);
        assertThat(status.stock())
                .as("DB 재고도 정확히 0")
                .isZero();
        assertThat(success.get()).isEqualTo(STOCK);
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("빈 포트를 찾지 못했습니다", e);
        }
    }

    private static boolean waitForPort(String host, int port, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 200);
                return true;
            } catch (IOException ignored) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
}
