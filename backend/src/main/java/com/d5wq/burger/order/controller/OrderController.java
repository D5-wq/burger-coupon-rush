package com.d5wq.burger.order.controller;

import com.d5wq.burger.common.response.ApiResponse;
import com.d5wq.burger.order.dto.OrderCreateRequest;
import com.d5wq.burger.order.dto.OrderResponse;
import com.d5wq.burger.order.service.OrderService;
import com.d5wq.burger.security.LoginUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> create(@LoginUser Long userId,
            @Valid @RequestBody OrderCreateRequest request) {
        return ApiResponse.success(orderService.createOrder(userId, request));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> myOrders(@LoginUser Long userId) {
        return ApiResponse.success(orderService.getMyOrders(userId));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOne(@LoginUser Long userId, @PathVariable Long orderId) {
        return ApiResponse.success(orderService.getOrder(userId, orderId));
    }
}
