package com.traditionalmarket.backend.order.presentation;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.traditionalmarket.backend.order.application.OrderService;
import com.traditionalmarket.backend.order.dto.response.OrderDetailResponse;
import com.traditionalmarket.backend.order.dto.response.OrderSummaryResponse;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailResponse createOrder(Principal principal) {
        return orderService.createOrder(principal.getName());
    }

    @GetMapping
    public List<OrderSummaryResponse> getOrders(Principal principal) {
        return orderService.getOrders(principal.getName());
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrder(
            Principal principal,
            @PathVariable @Positive Long orderId
    ) {
        return orderService.getOrder(principal.getName(), orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderDetailResponse cancelOrder(
            Principal principal,
            @PathVariable @Positive Long orderId
    ) {
        return orderService.cancelOrder(principal.getName(), orderId);
    }
}
