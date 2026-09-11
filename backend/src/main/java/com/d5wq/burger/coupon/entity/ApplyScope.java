package com.d5wq.burger.coupon.entity;

/**
 * 쿠폰이 적용되는 범위.
 * - ORDER: 전체 주문 금액에 할인 (예: 전체 주문 10%)
 * - PRODUCT: 특정 상품(버거)에만 할인 (예: 클래식 치즈버거 30%)
 */
public enum ApplyScope {
    ORDER,
    PRODUCT
}
