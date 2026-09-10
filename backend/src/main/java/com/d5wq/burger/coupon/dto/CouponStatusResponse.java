package com.d5wq.burger.coupon.dto;

import com.d5wq.burger.coupon.entity.Coupon;

/**
 * 쿠폰 현황. stock(남은 재고)과 issuedRows(실제 발급 내역 행 수)를 함께 노출해
 * 초과 발급 여부를 눈으로 확인할 수 있게 한다.
 */
public record CouponStatusResponse(
        Long couponId,
        String name,
        int discountRate,
        int totalQuantity,
        int stock,
        int issuedQuantity,
        long issuedRows) {

    public static CouponStatusResponse of(Coupon coupon, long issuedRows) {
        return new CouponStatusResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountRate(),
                coupon.getTotalQuantity(),
                coupon.getStock(),
                coupon.issuedQuantity(),
                issuedRows);
    }
}
