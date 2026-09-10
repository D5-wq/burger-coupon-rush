package com.d5wq.burger.coupon.controller;

import com.d5wq.burger.common.exception.BusinessException;
import com.d5wq.burger.common.exception.ErrorCode;
import com.d5wq.burger.common.response.ApiResponse;
import com.d5wq.burger.coupon.dto.CouponCreateRequest;
import com.d5wq.burger.coupon.dto.CouponStatusResponse;
import com.d5wq.burger.coupon.service.CouponIssueService;
import com.d5wq.burger.coupon.service.IssueStrategy;
import com.d5wq.burger.security.LoginUser;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponIssueService couponIssueService;

    /** 선착순 쿠폰 발급. strategy 로 동시성 제어 방식을 선택(naive/pessimistic/redis). */
    @PostMapping("/{couponId}/issue")
    public ApiResponse<Void> issue(
            @PathVariable Long couponId,
            @LoginUser Long userId,
            @RequestParam(required = false) String strategy) {
        couponIssueService.issue(couponId, userId, resolveStrategy(strategy));
        return ApiResponse.ok();
    }

    /** 쿠폰 현황(재고/발급수) 조회 — 초과 발급 확인용. 인증 불필요. */
    @GetMapping("/{couponId}/status")
    public ApiResponse<CouponStatusResponse> status(@PathVariable Long couponId) {
        return ApiResponse.success(couponIssueService.getStatus(couponId));
    }

    /** 쿠폰 생성 (테스트/시연용). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody CouponCreateRequest request) {
        Long id = couponIssueService.createCoupon(request);
        return ApiResponse.success(Map.of("couponId", id));
    }

    /** 재고/발급내역 초기화 (부하테스트 반복용). */
    @PostMapping("/{couponId}/reset")
    public ApiResponse<Void> reset(@PathVariable Long couponId) {
        couponIssueService.reset(couponId);
        return ApiResponse.ok();
    }

    private IssueStrategy resolveStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return couponIssueService.defaultStrategy();
        }
        try {
            return IssueStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "알 수 없는 전략: " + strategy);
        }
    }
}
