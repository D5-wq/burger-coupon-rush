package com.d5wq.burger.order.service;

import com.d5wq.burger.common.exception.BusinessException;
import com.d5wq.burger.common.exception.ErrorCode;
import com.d5wq.burger.order.dto.OrderCreateRequest;
import com.d5wq.burger.order.dto.OrderCreateRequest.Line;
import com.d5wq.burger.order.dto.OrderCreateRequest.OptionSelection;
import com.d5wq.burger.order.dto.OrderResponse;
import com.d5wq.burger.order.entity.Order;
import com.d5wq.burger.order.entity.OrderItem;
import com.d5wq.burger.order.entity.OrderItemOption;
import com.d5wq.burger.order.repository.OrderRepository;
import com.d5wq.burger.product.entity.OptionGroup;
import com.d5wq.burger.product.entity.OptionItem;
import com.d5wq.burger.product.entity.Product;
import com.d5wq.burger.product.repository.OptionGroupRepository;
import com.d5wq.burger.product.repository.ProductRepository;
import com.d5wq.burger.user.entity.User;
import com.d5wq.burger.user.repository.UserRepository;
import java.util.HashMap;
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
    private final OptionGroupRepository optionGroupRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        if (request.items().isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_ORDER);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 요청된 상품들을 한 번에 조회해 N+1을 피한다.
        List<Long> productIds = request.items().stream().map(Line::productId).toList();
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<OrderItem> items = request.items().stream()
                .map(line -> buildItem(productMap, line))
                .toList();

        Order order = Order.create(user, items);
        return OrderResponse.from(orderRepository.save(order));
    }

    /** 한 주문 라인을 만들면서 선택 옵션을 검증하고, 가격/이름을 서버 값으로 스냅샷한다. */
    private OrderItem buildItem(Map<Long, Product> productMap, Line line) {
        Product product = productMap.get(line.productId());
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        OrderItem item = OrderItem.of(product, line.quantity());

        // 상품에 정의된 옵션 그룹/항목을 기준으로만 선택을 허용한다(클라이언트 값 불신).
        List<OptionGroup> groups = optionGroupRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        Map<Long, OptionItem> itemById = new HashMap<>();
        Map<Long, OptionGroup> groupByItemId = new HashMap<>();
        for (OptionGroup group : groups) {
            for (OptionItem optionItem : group.getItems()) {
                itemById.put(optionItem.getId(), optionItem);
                groupByItemId.put(optionItem.getId(), group);
            }
        }

        Map<Long, Integer> selectedCountPerGroup = new HashMap<>();
        for (OptionSelection selection : line.options()) {
            OptionItem source = itemById.get(selection.optionItemId());
            if (source == null) {
                throw new BusinessException(ErrorCode.INVALID_OPTION);
            }
            if (selection.quantity() > source.getMaxQuantity()) {
                throw new BusinessException(ErrorCode.OPTION_QUANTITY_EXCEEDED);
            }
            OptionGroup group = groupByItemId.get(selection.optionItemId());
            selectedCountPerGroup.merge(group.getId(), 1, Integer::sum);
            item.addOption(OrderItemOption.of(source, selection.quantity()));
        }

        // 그룹 단위 규칙: 필수 그룹은 최소 선택 충족, 어떤 그룹도 최대 선택 초과 불가.
        for (OptionGroup group : groups) {
            int selected = selectedCountPerGroup.getOrDefault(group.getId(), 0);
            if (group.isRequired() && selected < Math.max(1, group.getMinSelect())) {
                throw new BusinessException(ErrorCode.REQUIRED_OPTION_MISSING);
            }
            if (selected > group.getMaxSelect()) {
                throw new BusinessException(ErrorCode.OPTION_SELECTION_INVALID);
            }
        }
        return item;
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
