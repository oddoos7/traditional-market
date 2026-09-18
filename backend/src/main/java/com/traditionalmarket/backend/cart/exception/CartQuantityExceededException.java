package com.traditionalmarket.backend.cart.exception;

public class CartQuantityExceededException extends RuntimeException {

    public CartQuantityExceededException(Long productId, int requestedQuantity, int availableQuantity) {
        super(
                "장바구니 수량이 구매 가능한 수량을 초과했습니다. productId="
                        + productId
                        + ", requestedQuantity="
                        + requestedQuantity
                        + ", availableQuantity="
                        + availableQuantity
        );
    }
}
