package com.d5wq.burger.coupon.service;

import com.d5wq.burger.common.exception.BusinessException;
import com.d5wq.burger.common.exception.ErrorCode;
import com.d5wq.burger.coupon.dto.CouponCreateRequest;
import com.d5wq.burger.coupon.dto.CouponStatusResponse;
import com.d5wq.burger.coupon.entity.Coupon;
import com.d5wq.burger.coupon.repository.CouponIssueRepository;
import com.d5wq.burger.coupon.repository.CouponRepository;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 쿠폰 발급 파사드. 전략({@link IssueStrategy})에 맞는 {@link CouponIssuer}를 골라 위임하고,
 * 쿠폰 생성/현황/리셋 같은 부수 기능을 제공한다.
 */
@Service
public class CouponIssueService {

    private final Map<IssueStrategy, CouponIssuer> issuers;
    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final IssueStrategy defaultStrategy;

    public CouponIssueService(
            List<CouponIssuer> issuerList,
            CouponRepository couponRepository,
            CouponIssueRepository couponIssueRepository,
            @Value("${coupon.issue-strategy:naive}") String defaultStrategy) {
        this.issuers = new EnumMap<>(IssueStrategy.class);
        issuerList.forEach(issuer -> this.issuers.put(issuer.strategy(), issuer));
        this.couponRepository = couponRepository;
        this.couponIssueRepository = couponIssueRepository;
        this.defaultStrategy = IssueStrategy.valueOf(defaultStrategy.toUpperCase());
    }

    /** 지정한 전략으로 발급. 구현이 없는 전략이면 예외. */
    public void issue(Long couponId, Long userId, IssueStrategy strategy) {
        CouponIssuer issuer = issuers.get(strategy);
        if (issuer == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "아직 구현되지 않은 발급 전략입니다: " + strategy);
        }
        issuer.issue(couponId, userId);
    }

    public IssueStrategy defaultStrategy() {
        return defaultStrategy;
    }

    @Transactional
    public Long createCoupon(CouponCreateRequest request) {
        LocalDateTime start = request.startAt() != null ? request.startAt() : LocalDateTime.now();
        LocalDateTime end = request.endAt() != null ? request.endAt() : start.plusDays(7);
        Coupon coupon = Coupon.create(
                request.name(), request.discountRate(), request.totalQuantity(), start, end);
        return couponRepository.save(coupon).getId();
    }

    @Transactional(readOnly = true)
    public CouponStatusResponse getStatus(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        return CouponStatusResponse.of(coupon, couponIssueRepository.countByCouponId(couponId));
    }

    /** 재고/발급내역 초기화 (부하테스트·버그 재현 반복용). */
    @Transactional
    public void reset(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        couponIssueRepository.deleteByCouponId(couponId);
        coupon.resetStock();
    }
}
