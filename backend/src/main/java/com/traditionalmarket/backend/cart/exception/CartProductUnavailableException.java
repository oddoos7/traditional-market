package com.traditionalmarket.backend.cart.exception;

public class CartProductUnavailableException extends RuntimeException {

    public CartProductUnavailableException(Long productId) {
        super("현재 장바구니에 담을 수 없는 상품입니다. productId=" + productId);
    }
}
