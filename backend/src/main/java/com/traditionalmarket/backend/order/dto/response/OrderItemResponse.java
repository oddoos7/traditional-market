package com.traditionalmarket.backend.order.dto.response;

import java.math.BigDecimal;

import com.traditionalmarket.backend.order.domain.OrderItem;

public record OrderItemResponse(
        Long orderItemId,
        Long productId,
        String productName,
        String productImageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineAmount
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getProductImageUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineAmount()
        );
    }
}
