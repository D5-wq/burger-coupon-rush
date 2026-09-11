package com.d5wq.burger.order.dto;

import com.d5wq.burger.order.entity.Order;
import com.d5wq.burger.order.entity.OrderItem;
import com.d5wq.burger.order.entity.OrderItemOption;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        List<Item> items,
        int totalPrice,
        int discountAmount,
        int finalPrice,
        String status,
        LocalDateTime createdAt) {

    public record Item(
            Long productId,
            String productName,
            int unitPrice,
            int quantity,
            int lineTotal,
            List<Option> options) {

        static Item from(OrderItem oi) {
            List<Option> options = oi.getOptions().stream()
                    .map(Option::from)
                    .toList();
            return new Item(
                    oi.getProduct().getId(),
                    oi.getProduct().getName(),
                    oi.getUnitPrice(),
                    oi.getQuantity(),
                    oi.lineTotal(),
                    options);
        }
    }

    public record Option(
            String groupName,
            String itemName,
            int extraPrice,
            int quantity) {

        static Option from(OrderItemOption o) {
            return new Option(o.getGroupName(), o.getItemName(), o.getExtraPrice(), o.getQuantity());
        }
    }

    public static OrderResponse from(Order order) {
        List<Item> items = order.getOrderItems().stream()
                .map(Item::from)
                .toList();
        return new OrderResponse(
                order.getId(),
                items,
                order.getTotalPrice(),
                order.getDiscountAmount(),
                order.getFinalPrice(),
                order.getStatus().name(),
                order.getCreatedAt());
    }
}
