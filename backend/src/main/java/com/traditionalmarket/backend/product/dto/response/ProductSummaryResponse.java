package com.traditionalmarket.backend.product.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.traditionalmarket.backend.product.domain.Product;
import com.traditionalmarket.backend.product.domain.ProductStatus;

public record ProductSummaryResponse(
        Long id,
        Long storeId,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        String imageUrl,
        ProductStatus status,
        LocalDateTime createdAt
) {

    public static ProductSummaryResponse from(Product product) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getStore().getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getStatus(),
                product.getCreatedAt()
        );
    }
}
