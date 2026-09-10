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
 * Step 4: DB 비관적 락(PESSIMISTIC_WRITE) 기반 발급.
 *
 * <p>naive와 로직은 똑같지만, 쿠폰을 {@code findByIdForUpdate}로 읽는다.
 * 이 순간 DB가 해당 행에 배타적 락을 걸어, "읽기~차감~커밋"을 <b>한 트랜잭션씩 직렬화</b>한다.
 * → 여러 스레드가 같은 재고를 동시에 읽는 상황 자체가 사라져 초과 발급이 0이 된다.
 */
@Component
@RequiredArgsConstructor
public class PessimisticLockCouponIssuer implements CouponIssuer {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;

    @Override
    @Transactional
    public void issue(Long couponId, Long userId) {
        // 락을 잡고 읽는다 → 이 행에 대해 다른 트랜잭션은 여기서 대기한다
        Coupon coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        if (!coupon.isOpen(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }
        if (couponIssueRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
        if (!coupon.hasStock()) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }
        coupon.decreaseStock();
        couponIssueRepository.save(CouponIssue.of(couponId, userId));
        // 트랜잭션 커밋 시점에 락 해제 → 다음 대기 트랜잭션이 진행
    }

    @Override
    public IssueStrategy strategy() {
        return IssueStrategy.PESSIMISTIC;
    }
}
