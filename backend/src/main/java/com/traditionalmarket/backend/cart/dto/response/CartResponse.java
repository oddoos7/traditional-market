package com.traditionalmarket.backend.cart.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.traditionalmarket.backend.cart.domain.Cart;
import com.traditionalmarket.backend.product.domain.Product;

public record CartResponse(
        Long cartId,
        Long marketId,
        String marketName,
        Long storeId,
        String storeName,
        List<CartItemResponse> items,
        int totalItemCount,
        BigDecimal totalAmount,
        boolean orderable
) {

    public static CartResponse empty() {
        return new CartResponse(
                null,
                null,
                null,
                null,
                null,
                List.of(),
                0,
                BigDecimal.ZERO.setScale(2),
                false
        );
    }

    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(CartItemResponse::from)
                .toList();

        if (items.isEmpty()) {
            return new CartResponse(
                    cart.getId(),
                    null,
                    null,
                    null,
                    null,
                    items,
                    0,
                    BigDecimal.ZERO.setScale(2),
                    false
            );
        }

        Product firstProduct = cart.getItems().get(0).getProduct();
        int totalItemCount = items.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();
        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean orderable = items.stream().allMatch(CartItemResponse::available);

        return new CartResponse(
                cart.getId(),
                firstProduct.getStore().getMarket().getId(),
                firstProduct.getStore().getMarket().getName(),
                firstProduct.getStore().getId(),
                firstProduct.getStore().getName(),
                items,
                totalItemCount,
                totalAmount,
                orderable
        );
    }
}
