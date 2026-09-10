package com.d5wq.burger.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import com.d5wq.burger.coupon.dto.CouponCreateRequest;
import com.d5wq.burger.coupon.dto.CouponStatusResponse;
import com.d5wq.burger.coupon.service.CouponIssueService;
import com.d5wq.burger.coupon.service.IssueStrategy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Step 4: 비관적 락 발급이 동일 조건에서 <b>정확히 재고만큼만</b> 발급함을 검증한다.
 * (naive 버전과 완전히 같은 부하 → 결과만 968 → 100 으로 바뀐다)
 */
@ActiveProfiles("test")
@SpringBootTest
class PessimisticCouponConcurrencyTest {

    private static final int STOCK = 100;
    private static final int USERS = 1000;
    private static final int THREADS = 50;

    @Autowired
    CouponIssueService couponIssueService;

    @Test
    @DisplayName("비관적 락: 재고 100 + 동시 1000요청 → 정확히 100장만 발급, 초과 0")
    void pessimistic_noOverIssue_underConcurrency() throws InterruptedException {
        Long couponId = couponIssueService.createCoupon(
                new CouponCreateRequest("동시성 테스트 쿠폰(락)", 20, STOCK, null, null));

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
                    couponIssueService.issue(couponId, userId, IssueStrategy.PESSIMISTIC);
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
        System.out.println("========== [비관적 락 결과] ==========");
        System.out.println(" 총 수량(totalQuantity) : " + status.totalQuantity());
        System.out.println(" 남은 재고(stock)        : " + status.stock());
        System.out.println(" 발급 성공 응답 수        : " + success.get());
        System.out.println(" 품절 응답 수             : " + soldOut.get());
        System.out.println(" 기타 실패 수             : " + other.get());
        System.out.println(" 실제 발급 내역 행 수     : " + status.issuedRows() + "  (기대: " + STOCK + ")");
        System.out.println(" 초과 발급               : " + (status.issuedRows() - STOCK) + " 장");
        System.out.println("=====================================");

        assertThat(status.issuedRows())
                .as("비관적 락은 정확히 재고만큼만 발급한다")
                .isEqualTo(STOCK);
        assertThat(status.stock())
                .as("재고는 정확히 0")
                .isZero();
        assertThat(success.get()).isEqualTo(STOCK);
    }
}
