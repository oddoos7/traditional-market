package com.traditionalmarket.backend.order.exception;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("주문할 장바구니 상품이 없습니다.");
    }
}
