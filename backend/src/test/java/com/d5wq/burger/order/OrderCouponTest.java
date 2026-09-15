package com.d5wq.burger.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.d5wq.burger.common.exception.BusinessException;
import com.d5wq.burger.common.exception.ErrorCode;
import com.d5wq.burger.coupon.dto.CouponCreateRequest;
import com.d5wq.burger.coupon.entity.ApplyScope;
import com.d5wq.burger.coupon.service.CouponIssueService;
import com.d5wq.burger.coupon.service.IssueStrategy;
import com.d5wq.burger.order.dto.OrderCreateRequest;
import com.d5wq.burger.order.dto.OrderCreateRequest.Line;
import com.d5wq.burger.order.dto.OrderCreateRequest.OptionSelection;
import com.d5wq.burger.order.dto.OrderResponse;
import com.d5wq.burger.order.service.OrderService;
import com.d5wq.burger.product.dto.OptionGroupResponse;
import com.d5wq.burger.product.dto.ProductDetailResponse;
import com.d5wq.burger.product.dto.ProductResponse;
import com.d5wq.burger.product.service.ProductService;
import com.d5wq.burger.user.dto.SignUpRequest;
import com.d5wq.burger.user.dto.UserResponse;
import com.d5wq.burger.user.service.AuthService;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 주문에 쿠폰을 적용할 때 범위별 할인 계산과 유효성(보유/중복사용/대상상품)을 검증한다. (#17)
 */
@ActiveProfiles("test")
@SpringBootTest
class OrderCouponTest {

    @Autowired
    AuthService authService;
    @Autowired
    ProductService productService;
    @Autowired
    OrderService orderService;
    @Autowired
    CouponIssueService couponIssueService;

    @Test
    @DisplayName("ORDER 범위 10% 쿠폰 → 주문 합계의 10%가 할인된다")
    void orderScope_discountsWholeOrder() {
        UserResponse user = signUp("coupon-order@test.com");
        ProductResponse product = productAt(0);
        Long couponId = couponIssueService.createCoupon(
                new CouponCreateRequest("전체 10%", 10, ApplyScope.ORDER, null, 100, null, null));
        couponIssueService.issue(couponId, user.id(), IssueStrategy.NAIVE);

        OrderResponse order = orderService.createOrder(user.id(),
                new OrderCreateRequest(List.of(danpumLine(product, 2)), couponId));

        int base = product.price() * 2;
        int expectedDiscount = base * 10 / 100;
        assertThat(order.totalPrice()).isEqualTo(base);
        assertThat(order.discountAmount()).isEqualTo(expectedDiscount);
        assertThat(order.finalPrice()).isEqualTo(base - expectedDiscount);
    }

    @Test
    @DisplayName("PRODUCT 범위 쿠폰 → 대상 상품 라인에만 할인, 다른 상품은 그대로")
    void productScope_discountsOnlyTargetLine() {
        UserResponse user = signUp("coupon-product@test.com");
        ProductResponse target = productAt(0);
        ProductResponse other = productAt(1);
        Long couponId = couponIssueService.createCoupon(new CouponCreateRequest(
                target.name() + " 30%", 30, ApplyScope.PRODUCT, target.id(), 100, null, null));
        couponIssueService.issue(couponId, user.id(), IssueStrategy.NAIVE);

        OrderResponse order = orderService.createOrder(user.id(), new OrderCreateRequest(
                List.of(danpumLine(target, 1), danpumLine(other, 1)), couponId));

        int total = target.price() + other.price();
        int expectedDiscount = target.price() * 30 / 100; // 대상 상품 라인 기준
        assertThat(order.totalPrice()).isEqualTo(total);
        assertThat(order.discountAmount()).isEqualTo(expectedDiscount);
        assertThat(order.finalPrice()).isEqualTo(total - expectedDiscount);
    }

    @Test
    @DisplayName("같은 쿠폰을 두 번 사용하면 두 번째 주문은 COUPON_ALREADY_USED")
    void reuse_isBlocked() {
        UserResponse user = signUp("coupon-reuse@test.com");
        ProductResponse product = productAt(0);
        Long couponId = couponIssueService.createCoupon(
                new CouponCreateRequest("전체 10%", 10, ApplyScope.ORDER, null, 100, null, null));
        couponIssueService.issue(couponId, user.id(), IssueStrategy.NAIVE);

        orderService.createOrder(user.id(),
                new OrderCreateRequest(List.of(danpumLine(product, 1)), couponId));

        assertBusiness(
                () -> orderService.createOrder(user.id(),
                        new OrderCreateRequest(List.of(danpumLine(product, 1)), couponId)),
                ErrorCode.COUPON_ALREADY_USED);
    }

    @Test
    @DisplayName("발급받지 않은 쿠폰을 적용하면 COUPON_NOT_ISSUED")
    void notIssued_isRejected() {
        UserResponse user = signUp("coupon-notmine@test.com");
        ProductResponse product = productAt(0);
        Long couponId = couponIssueService.createCoupon(
                new CouponCreateRequest("전체 10%", 10, ApplyScope.ORDER, null, 100, null, null));
        // 발급하지 않음

        assertBusiness(
                () -> orderService.createOrder(user.id(),
                        new OrderCreateRequest(List.of(danpumLine(product, 1)), couponId)),
                ErrorCode.COUPON_NOT_ISSUED);
    }

    @Test
    @DisplayName("PRODUCT 쿠폰의 대상 상품이 주문에 없으면 COUPON_NOT_APPLICABLE")
    void productScope_targetMissing_isRejected() {
        UserResponse user = signUp("coupon-nomatch@test.com");
        ProductResponse target = productAt(0);
        ProductResponse ordered = productAt(1);
        Long couponId = couponIssueService.createCoupon(new CouponCreateRequest(
                target.name() + " 30%", 30, ApplyScope.PRODUCT, target.id(), 100, null, null));
        couponIssueService.issue(couponId, user.id(), IssueStrategy.NAIVE);

        assertBusiness(
                () -> orderService.createOrder(user.id(),
                        new OrderCreateRequest(List.of(danpumLine(ordered, 1)), couponId)),
                ErrorCode.COUPON_NOT_APPLICABLE);
    }

    // --- helpers ---

    private UserResponse signUp(String email) {
        return authService.signUp(new SignUpRequest(email, "pass1234", "쿠폰구매자"));
    }

    private ProductResponse productAt(int index) {
        return productService.getProducts().get(index);
    }

    /** 필수 '구성' 그룹을 만족시키기 위해 '단품'을 선택한 라인. */
    private Line danpumLine(ProductResponse product, int quantity) {
        return new Line(product.id(), quantity, List.of(new OptionSelection(danpumId(product.id()), 1)));
    }

    private Long danpumId(Long productId) {
        ProductDetailResponse detail = productService.getProduct(productId);
        return detail.optionGroups().stream()
                .map(OptionGroupResponse::items)
                .flatMap(List::stream)
                .filter(item -> item.name().equals("단품"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("단품 옵션을 찾을 수 없습니다."))
                .id();
    }

    private void assertBusiness(ThrowingCallable callable, ErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
