package com.d5wq.burger.coupon.dto;

import com.d5wq.burger.coupon.entity.ApplyScope;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record CouponCreateRequest(
        @NotBlank String name,
        @Min(1) @Max(100) int discountRate,
        ApplyScope applyScope,   // null 이면 ORDER 로 간주
        Long productId,          // applyScope=PRODUCT 일 때 대상 상품
        @Min(1) int totalQuantity,
        LocalDateTime startAt,
        LocalDateTime endAt) {
}
