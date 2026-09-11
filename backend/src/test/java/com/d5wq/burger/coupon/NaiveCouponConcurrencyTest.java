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
 * Step 3: naive 발급의 동시성 버그를 재현한다.
 *
 * <p>재고 100개인 쿠폰에 서로 다른 유저 1000명이 "동시에" 발급을 시도한다.
 * 락이 없으므로 여러 스레드가 같은 재고를 읽고 모두 통과 → <b>초과 발급</b>이 발생한다.
 */
@ActiveProfiles("test")
@SpringBootTest
class NaiveCouponConcurrencyTest {

    private static final int STOCK = 100;
    private static final int USERS = 1000;
    private static final int THREADS = 50;

    @Autowired
    CouponIssueService couponIssueService;

    @Test
    @DisplayName("naive: 재고 100 + 동시 1000요청 → 발급 내역이 100을 초과한다(초과 발급)")
    void naive_overIssues_underConcurrency() throws InterruptedException {
        // given: 재고 100짜리 쿠폰
        Long couponId = couponIssueService.createCoupon(
                new CouponCreateRequest("동시성 테스트 쿠폰", 20, null, null, STOCK, null, null));

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);   // 모든 스레드를 동시에 출발시키는 신호
        CountDownLatch done = new CountDownLatch(USERS);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        // when: 1000명이 동시에 발급 시도
        for (int i = 0; i < USERS; i++) {
            long userId = i + 1;
            pool.submit(() -> {
                try {
                    startGate.await();               // 신호 대기 → 최대한 동시에 출발
                    couponIssueService.issue(couponId, userId, IssueStrategy.NAIVE);
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
        startGate.countDown();  // 출발!
        done.await();
        pool.shutdown();

        // then: 현황 확인
        CouponStatusResponse status = couponIssueService.getStatus(couponId);
        System.out.println("========== [naive 결과] ==========");
        System.out.println(" 총 수량(totalQuantity) : " + status.totalQuantity());
        System.out.println(" 남은 재고(stock)        : " + status.stock());
        System.out.println(" 발급 성공 응답 수        : " + success.get());
        System.out.println(" 품절 응답 수             : " + soldOut.get());
        System.out.println(" 기타 실패 수             : " + other.get());
        System.out.println(" 실제 발급 내역 행 수     : " + status.issuedRows() + "  (정상이면 " + STOCK + " 이어야 함)");
        System.out.println(" 초과 발급               : " + (status.issuedRows() - STOCK) + " 장");
        System.out.println("==================================");

        // 버그 증명: 재고(100)보다 많이 발급됐다
        assertThat(status.issuedRows())
                .as("naive 버전은 동시성 제어가 없어 재고보다 많이 발급된다")
                .isGreaterThan(STOCK);
    }
}
