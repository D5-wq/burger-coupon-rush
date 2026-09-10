package com.d5wq.burger.coupon.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record CouponCreateRequest(
        @NotBlank String name,
        @Min(1) @Max(100) int discountRate,
        @Min(1) int totalQuantity,
        LocalDateTime startAt,
        LocalDateTime endAt) {
}
