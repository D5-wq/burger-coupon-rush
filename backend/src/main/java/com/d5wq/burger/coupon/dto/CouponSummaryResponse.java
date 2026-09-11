package com.d5wq.burger.coupon.dto;

import com.d5wq.burger.coupon.entity.Coupon;
import java.time.LocalDateTime;

/** 발급 가능한 쿠폰 목록용 요약. */
public record CouponSummaryResponse(
        Long couponId,
        String name,
        int discountRate,
        String applyScope,      // ORDER | PRODUCT
        Long productId,         // PRODUCT 범위일 때 대상 상품
        String productName,     // 대상 상품 이름 (ORDER면 null)
        int totalQuantity,
        int stock,
        boolean soldOut,
        boolean open) {

    public static CouponSummaryResponse from(Coupon coupon, String productName) {
        return new CouponSummaryResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountRate(),
                coupon.getApplyScope().name(),
                coupon.getProductId(),
                productName,
                coupon.getTotalQuantity(),
                coupon.getStock(),
                !coupon.hasStock(),
                coupon.isOpen(LocalDateTime.now()));
    }
}
