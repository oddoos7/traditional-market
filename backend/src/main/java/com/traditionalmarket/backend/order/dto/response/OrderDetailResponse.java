package com.traditionalmarket.backend.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.traditionalmarket.backend.order.domain.OrderStatus;
import com.traditionalmarket.backend.order.domain.PurchaseOrder;

public record OrderDetailResponse(
        Long id,
        String orderNumber,
        Long marketId,
        String marketName,
        Long storeId,
        String storeName,
        OrderStatus status,
        List<OrderItemResponse> items,
        int totalItemCount,
        BigDecimal totalAmount,
        LocalDateTime canceledAt,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OrderDetailResponse from(PurchaseOrder order) {
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStore().getMarket().getId(),
                order.getStore().getMarket().getName(),
                order.getStore().getId(),
                order.getStore().getName(),
                order.getStatus(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getTotalItemCount(),
                order.getTotalAmount(),
                order.getCanceledAt(),
                order.getPaidAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
