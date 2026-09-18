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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Tag(name = "주문", description = "주문 생성, 조회 및 결제 전 주문 취소 API")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "장바구니로 주문 생성")
    public OrderDetailResponse createOrder(Principal principal) {
        return orderService.createOrder(principal.getName());
    }

    @GetMapping
    @Operation(summary = "내 주문 목록 조회")
    public List<OrderSummaryResponse> getOrders(Principal principal) {
        return orderService.getOrders(principal.getName());
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "내 주문 상세 조회")
    public OrderDetailResponse getOrder(
            Principal principal,
            @PathVariable @Positive Long orderId
    ) {
        return orderService.getOrder(principal.getName(), orderId);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "결제 전 주문 취소")
    public OrderDetailResponse cancelOrder(
            Principal principal,
            @PathVariable @Positive Long orderId
    ) {
        return orderService.cancelOrder(principal.getName(), orderId);
    }
}
