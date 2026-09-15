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
        Long productId,         // PRODUCT 범위일 때 대상 상품 (주문 적용 가능 여부 판단용)
        String productName,     // 대상 상품 이름 (ORDER면 null)
        boolean used,           // 주문에 이미 사용했는지
        LocalDateTime issuedAt) {

    public static MyCouponResponse of(CouponIssue issue, Coupon coupon, String productName) {
        return new MyCouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountRate(),
                coupon.getApplyScope().name(),
                coupon.getProductId(),
                productName,
                issue.isUsed(),
                issue.getCreatedAt());
    }
}
