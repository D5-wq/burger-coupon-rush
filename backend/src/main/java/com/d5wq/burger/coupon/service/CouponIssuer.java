package com.d5wq.burger.coupon.service;

/**
 * 쿠폰 발급 전략의 공통 인터페이스.
 * 같은 "발급" 기능을 여러 동시성 제어 방식으로 구현해 비교한다.
 */
public interface CouponIssuer {

    /**
     * 유저에게 쿠폰을 1장 발급한다.
     * 실패 시 BusinessException(COUPON_SOLD_OUT / COUPON_ALREADY_ISSUED / COUPON_EXPIRED).
     */
    void issue(Long couponId, Long userId);

    /** 이 구현이 담당하는 전략. */
    IssueStrategy strategy();
}
