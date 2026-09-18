package com.traditionalmarket.backend.cart.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long cartItemId) {
        super("장바구니 상품을 찾을 수 없습니다. cartItemId=" + cartItemId);
    }
}
