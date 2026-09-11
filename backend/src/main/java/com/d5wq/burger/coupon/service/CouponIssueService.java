package com.d5wq.burger.coupon.service;

import com.d5wq.burger.common.exception.BusinessException;
import com.d5wq.burger.common.exception.ErrorCode;
import com.d5wq.burger.coupon.dto.CouponCreateRequest;
import com.d5wq.burger.coupon.dto.CouponStatusResponse;
import com.d5wq.burger.coupon.dto.CouponSummaryResponse;
import com.d5wq.burger.coupon.dto.MyCouponResponse;
import com.d5wq.burger.coupon.entity.ApplyScope;
import com.d5wq.burger.coupon.entity.Coupon;
import com.d5wq.burger.coupon.entity.CouponIssue;
import com.d5wq.burger.coupon.repository.CouponIssueRepository;
import com.d5wq.burger.coupon.repository.CouponRepository;
import com.d5wq.burger.product.entity.Product;
import com.d5wq.burger.product.repository.ProductRepository;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final ProductRepository productRepository;
    private final IssueStrategy defaultStrategy;

    public CouponIssueService(
            List<CouponIssuer> issuerList,
            CouponRepository couponRepository,
            CouponIssueRepository couponIssueRepository,
            ProductRepository productRepository,
            @Value("${coupon.issue-strategy:naive}") String defaultStrategy) {
        this.issuers = new EnumMap<>(IssueStrategy.class);
        issuerList.forEach(issuer -> this.issuers.put(issuer.strategy(), issuer));
        this.couponRepository = couponRepository;
        this.couponIssueRepository = couponIssueRepository;
        this.productRepository = productRepository;
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
        ApplyScope scope = request.applyScope() != null ? request.applyScope() : ApplyScope.ORDER;
        Coupon coupon = (scope == ApplyScope.PRODUCT)
                ? Coupon.forProduct(request.name(), request.discountRate(), request.productId(),
                        request.totalQuantity(), start, end)
                : Coupon.forOrder(request.name(), request.discountRate(),
                        request.totalQuantity(), start, end);
        return couponRepository.save(coupon).getId();
    }

    /** 발급 가능한 쿠폰 목록 (대상 상품 이름 포함). */
    @Transactional(readOnly = true)
    public List<CouponSummaryResponse> getIssuableCoupons() {
        List<Coupon> coupons = couponRepository.findAll();
        Map<Long, String> productNames = productNamesFor(
                coupons.stream().map(Coupon::getProductId).filter(id -> id != null).toList());
        return coupons.stream()
                .map(c -> CouponSummaryResponse.from(c, productNames.get(c.getProductId())))
                .toList();
    }

    /** 내 쿠폰함. */
    @Transactional(readOnly = true)
    public List<MyCouponResponse> getMyCoupons(Long userId) {
        List<CouponIssue> issues = couponIssueRepository.findByUserIdOrderByIdDesc(userId);
        if (issues.isEmpty()) {
            return List.of();
        }
        Map<Long, Coupon> coupons = couponRepository
                .findAllById(issues.stream().map(CouponIssue::getCouponId).distinct().toList())
                .stream().collect(Collectors.toMap(Coupon::getId, Function.identity()));
        Map<Long, String> productNames = productNamesFor(
                coupons.values().stream().map(Coupon::getProductId).filter(id -> id != null).toList());
        return issues.stream()
                .map(issue -> {
                    Coupon coupon = coupons.get(issue.getCouponId());
                    if (coupon == null) {
                        return null;
                    }
                    return MyCouponResponse.of(issue, coupon, productNames.get(coupon.getProductId()));
                })
                .filter(r -> r != null)
                .toList();
    }

    private Map<Long, String> productNamesFor(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
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
