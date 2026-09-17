package com.traditionalmarket.backend.product.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.traditionalmarket.backend.product.domain.Product;
import com.traditionalmarket.backend.product.domain.ProductStatus;

public record ProductDetailResponse(
        Long id,
        Long storeId,
        String storeName,
        Long marketId,
        String marketName,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        String imageUrl,
        ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getStore().getId(),
                product.getStore().getName(),
                product.getStore().getMarket().getId(),
                product.getStore().getMarket().getName(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
