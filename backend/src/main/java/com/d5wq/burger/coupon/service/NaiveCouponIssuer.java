package com.d5wq.burger.coupon.service;

import com.d5wq.burger.common.exception.BusinessException;
import com.d5wq.burger.common.exception.ErrorCode;
import com.d5wq.burger.coupon.entity.Coupon;
import com.d5wq.burger.coupon.entity.CouponIssue;
import com.d5wq.burger.coupon.repository.CouponIssueRepository;
import com.d5wq.burger.coupon.repository.CouponRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Step 3: 락 없는 naive 발급. <b>동시성 버그가 있는 버전</b>이다(의도적).
 *
 * <p>문제의 핵심: "재고 확인(③) → 차감(④)"이 하나의 원자적 연산이 아니다.
 * 여러 스레드가 동시에 ①에서 같은 stock 값을 읽으면, 모두 ③의 {@code stock > 0} 검사를
 * 통과한 뒤 각자 차감/발급을 진행한다 → 재고보다 많이 발급되는 <b>초과 발급</b>이 발생한다.
 *
 * <p>게다가 JPA 변경 감지는 커밋 시 {@code stock = (읽은 값 - 1)} 이라는 <b>절대값</b>으로
 * UPDATE를 날리기 때문에, 동시에 같은 값을 읽은 트랜잭션들의 차감이 서로 덮어써져
 * 사라진다(lost update).
 */
@Component
@RequiredArgsConstructor
public class NaiveCouponIssuer implements CouponIssuer {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;

    @Override
    @Transactional
    public void issue(Long couponId, Long userId) {
        // ① 쿠폰 읽기 (이 시점의 stock 을 메모리로 가져온다 — 락 없음)
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        // (발급 기간 확인)
        if (!coupon.isOpen(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }

        // ② 이미 받았나? (유저 중복 방지 — 애플리케이션 레벨 확인)
        if (couponIssueRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        // ③ 재고 확인 ─┐  이 두 줄 사이에 다른 스레드가 끼어들면(경쟁 상태)
        if (!coupon.hasStock()) { //  │  여러 스레드가 같은 stock 을 보고 모두 통과한다
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }
        // ④ 재고 차감 ─┘  (커밋 시 stock = 읽은값-1 절대값으로 UPDATE → lost update)
        coupon.decreaseStock();

        // ⑤ 발급 기록
        couponIssueRepository.save(CouponIssue.of(couponId, userId));
    }

    @Override
    public IssueStrategy strategy() {
        return IssueStrategy.NAIVE;
    }
}
