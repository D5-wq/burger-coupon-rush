package com.d5wq.burger.order.service;

import com.d5wq.burger.common.exception.BusinessException;
import com.d5wq.burger.common.exception.ErrorCode;
import com.d5wq.burger.order.dto.OrderCreateRequest;
import com.d5wq.burger.order.dto.OrderResponse;
import com.d5wq.burger.order.entity.Order;
import com.d5wq.burger.order.entity.OrderItem;
import com.d5wq.burger.order.repository.OrderRepository;
import com.d5wq.burger.product.entity.Product;
import com.d5wq.burger.product.repository.ProductRepository;
import com.d5wq.burger.user.entity.User;
import com.d5wq.burger.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        if (request.items().isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_ORDER);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 요청된 상품들을 한 번에 조회해 N+1을 피한다.
        List<Long> productIds = request.items().stream().map(OrderCreateRequest.Line::productId).toList();
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<OrderItem> items = request.items().stream()
                .map(line -> {
                    Product product = productMap.get(line.productId());
                    if (product == null) {
                        throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
                    }
                    return OrderItem.of(product, line.quantity());
                })
                .toList();

        Order order = Order.create(user, items);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findWithUserById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return OrderResponse.from(order);
    }
}
