package com.d5wq.burger.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.d5wq.burger.common.exception.BusinessException;
import com.d5wq.burger.order.dto.OrderCreateRequest;
import com.d5wq.burger.order.dto.OrderResponse;
import com.d5wq.burger.order.service.OrderService;
import com.d5wq.burger.product.dto.ProductResponse;
import com.d5wq.burger.product.service.ProductService;
import com.d5wq.burger.user.dto.SignUpRequest;
import com.d5wq.burger.user.dto.UserResponse;
import com.d5wq.burger.user.service.AuthService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Step 1 완료 조건: 회원가입 → 상품 조회 → 주문 생성 흐름 검증.
 */
@ActiveProfiles("test")
@SpringBootTest
class OrderFlowTest {

    @Autowired
    AuthService authService;
    @Autowired
    ProductService productService;
    @Autowired
    OrderService orderService;

    @Test
    @DisplayName("회원가입 후 상품을 조회하고 주문을 생성하면 합계가 올바르게 계산된다")
    void signUp_browse_order() {
        // given: 회원가입
        UserResponse user = authService.signUp(
                new SignUpRequest("buyer@test.com", "pass1234", "구매자"));

        // and: 시드된 상품 조회
        List<ProductResponse> products = productService.getProducts();
        assertThat(products).isNotEmpty();
        ProductResponse first = products.get(0);
        ProductResponse second = products.get(1);

        // when: 첫 상품 2개 + 두 번째 상품 1개 주문
        OrderResponse order = orderService.createOrder(user.id(), new OrderCreateRequest(List.of(
                new OrderCreateRequest.Line(first.id(), 2),
                new OrderCreateRequest.Line(second.id(), 1))));

        // then
        int expectedTotal = first.price() * 2 + second.price();
        assertThat(order.items()).hasSize(2);
        assertThat(order.totalPrice()).isEqualTo(expectedTotal);
        assertThat(order.finalPrice()).isEqualTo(expectedTotal);
        assertThat(order.status()).isEqualTo("CREATED");
    }

    @Test
    @DisplayName("존재하지 않는 상품을 주문하면 예외가 발생한다")
    void order_with_unknown_product_fails() {
        UserResponse user = authService.signUp(
                new SignUpRequest("buyer2@test.com", "pass1234", "구매자2"));

        assertThatThrownBy(() -> orderService.createOrder(user.id(), new OrderCreateRequest(List.of(
                new OrderCreateRequest.Line(999_999L, 1)))))
                .isInstanceOf(BusinessException.class);
    }
}
