package com.traditionalmarket.backend.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.traditionalmarket.backend.order.domain.OrderStatus;
import com.traditionalmarket.backend.order.domain.PurchaseOrder;

public record OrderSummaryResponse(
        Long id,
        String orderNumber,
        Long marketId,
        String marketName,
        Long storeId,
        String storeName,
        OrderStatus status,
        int totalItemCount,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {

    public static OrderSummaryResponse from(PurchaseOrder order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStore().getMarket().getId(),
                order.getStore().getMarket().getName(),
                order.getStore().getId(),
                order.getStore().getName(),
                order.getStatus(),
                order.getTotalItemCount(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }
}
