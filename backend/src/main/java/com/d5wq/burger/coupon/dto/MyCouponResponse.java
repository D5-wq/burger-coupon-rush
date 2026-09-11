package com.d5wq.burger.coupon.dto;

import com.d5wq.burger.coupon.entity.Coupon;
import com.d5wq.burger.coupon.entity.CouponIssue;
import java.time.LocalDateTime;

/** 내 쿠폰함 항목. */
public record MyCouponResponse(
        Long couponId,
        String name,
        int discountRate,
        String applyScope,      // ORDER | PRODUCT
        String productName,     // 대상 상품 이름 (ORDER면 null)
        LocalDateTime issuedAt) {

    public static MyCouponResponse of(CouponIssue issue, Coupon coupon, String productName) {
        return new MyCouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountRate(),
                coupon.getApplyScope().name(),
                productName,
                issue.getCreatedAt());
    }
}
