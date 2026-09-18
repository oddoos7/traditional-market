package com.traditionalmarket.backend.cart.dto.response;

import java.math.BigDecimal;

import com.traditionalmarket.backend.cart.domain.CartItem;
import com.traditionalmarket.backend.product.domain.Product;
import com.traditionalmarket.backend.product.domain.ProductStatus;

public record CartItemResponse(
        Long cartItemId,
        Long productId,
        String name,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineAmount,
        int stockQuantity,
        String imageUrl,
        ProductStatus productStatus,
        boolean available
) {

    public static CartItemResponse from(CartItem item) {
        Product product = item.getProduct();
        boolean available = product.getStatus() == ProductStatus.ON_SALE
                && product.getStockQuantity() >= item.getQuantity();

        return new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getPrice(),
                item.getQuantity(),
                product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getStatus(),
                available
        );
    }
}
