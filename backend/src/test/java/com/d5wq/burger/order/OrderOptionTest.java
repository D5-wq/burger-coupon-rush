package com.d5wq.burger.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.d5wq.burger.common.exception.BusinessException;
import com.d5wq.burger.common.exception.ErrorCode;
import com.d5wq.burger.order.dto.OrderCreateRequest;
import com.d5wq.burger.order.dto.OrderCreateRequest.Line;
import com.d5wq.burger.order.dto.OrderCreateRequest.OptionSelection;
import com.d5wq.burger.order.dto.OrderResponse;
import com.d5wq.burger.order.service.OrderService;
import com.d5wq.burger.product.dto.OptionGroupResponse;
import com.d5wq.burger.product.dto.OptionItemResponse;
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
 * 주문에 선택 옵션(세트/재료 추가 등)이 반영될 때
 * 서버가 라인 가격을 계산하고 옵션 규칙을 검증하는지 확인한다.
 */
@ActiveProfiles("test")
@SpringBootTest
class OrderOptionTest {

    @Autowired
    AuthService authService;
    @Autowired
    ProductService productService;
    @Autowired
    OrderService orderService;

    @Test
    @DisplayName("세트 구성 + 패티 2개 추가를 선택하면 라인가 = (기본가 + 세트 + 패티x2) x 수량")
    void option_price_reflected_in_line_total() {
        UserResponse user = signUp("opt-price@test.com");
        ProductResponse product = firstProduct();
        int base = product.price();

        Long setId = optionItemId(product.id(), "세트 (사이드+음료)");
        int setExtra = optionExtraPrice(product.id(), "세트 (사이드+음료)");
        Long pattyId = optionItemId(product.id(), "패티 추가");
        int pattyExtra = optionExtraPrice(product.id(), "패티 추가");

        OrderResponse order = orderService.createOrder(user.id(), new OrderCreateRequest(List.of(
                new Line(product.id(), 2, List.of(
                        new OptionSelection(setId, 1),
                        new OptionSelection(pattyId, 2))))));

        int expectedPerUnit = base + setExtra + pattyExtra * 2;
        assertThat(order.totalPrice()).isEqualTo(expectedPerUnit * 2);
        assertThat(order.items()).hasSize(1);
        assertThat(order.items().get(0).options()).hasSize(2);
        assertThat(order.items().get(0).lineTotal()).isEqualTo(expectedPerUnit * 2);
    }

    @Test
    @DisplayName("필수 구성(단품/세트)을 선택하지 않으면 REQUIRED_OPTION_MISSING")
    void required_group_missing_fails() {
        UserResponse user = signUp("opt-required@test.com");
        ProductResponse product = firstProduct();

        assertBusiness(
                () -> orderService.createOrder(user.id(), new OrderCreateRequest(List.of(
                        new Line(product.id(), 1, List.of())))),
                ErrorCode.REQUIRED_OPTION_MISSING);
    }

    @Test
    @DisplayName("옵션 수량이 최대치를 넘으면 OPTION_QUANTITY_EXCEEDED")
    void quantity_over_max_fails() {
        UserResponse user = signUp("opt-qty@test.com");
        ProductResponse product = firstProduct();
        Long setId = optionItemId(product.id(), "세트 (사이드+음료)");
        Long pattyId = optionItemId(product.id(), "패티 추가"); // maxQuantity = 2

        assertBusiness(
                () -> orderService.createOrder(user.id(), new OrderCreateRequest(List.of(
                        new Line(product.id(), 1, List.of(
                                new OptionSelection(setId, 1),
                                new OptionSelection(pattyId, 3)))))),
                ErrorCode.OPTION_QUANTITY_EXCEEDED);
    }

    @Test
    @DisplayName("단일 선택 그룹에서 두 항목을 고르면 OPTION_SELECTION_INVALID")
    void single_group_over_select_fails() {
        UserResponse user = signUp("opt-count@test.com");
        ProductResponse product = firstProduct();
        Long danpumId = optionItemId(product.id(), "단품");
        Long setId = optionItemId(product.id(), "세트 (사이드+음료)");

        assertBusiness(
                () -> orderService.createOrder(user.id(), new OrderCreateRequest(List.of(
                        new Line(product.id(), 1, List.of(
                                new OptionSelection(danpumId, 1),
                                new OptionSelection(setId, 1)))))),
                ErrorCode.OPTION_SELECTION_INVALID);
    }

    @Test
    @DisplayName("상품에 없는 옵션을 선택하면 INVALID_OPTION")
    void unknown_option_fails() {
        UserResponse user = signUp("opt-unknown@test.com");
        ProductResponse product = firstProduct();
        Long danpumId = optionItemId(product.id(), "단품");

        assertBusiness(
                () -> orderService.createOrder(user.id(), new OrderCreateRequest(List.of(
                        new Line(product.id(), 1, List.of(
                                new OptionSelection(danpumId, 1),
                                new OptionSelection(999_999L, 1)))))),
                ErrorCode.INVALID_OPTION);
    }

    // --- helpers ---

    private UserResponse signUp(String email) {
        return authService.signUp(new SignUpRequest(email, "pass1234", "옵션구매자"));
    }

    private ProductResponse firstProduct() {
        return productService.getProducts().get(0);
    }

    private OptionItemResponse findItem(Long productId, String itemName) {
        ProductDetailResponse detail = productService.getProduct(productId);
        return detail.optionGroups().stream()
                .map(OptionGroupResponse::items)
                .flatMap(List::stream)
                .filter(item -> item.name().equals(itemName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("옵션 항목을 찾을 수 없습니다: " + itemName));
    }

    private Long optionItemId(Long productId, String itemName) {
        return findItem(productId, itemName).id();
    }

    private int optionExtraPrice(Long productId, String itemName) {
        return findItem(productId, itemName).extraPrice();
    }

    private void assertBusiness(ThrowingCallable callable, ErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
