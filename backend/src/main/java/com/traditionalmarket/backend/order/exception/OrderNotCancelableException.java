package com.traditionalmarket.backend.order.exception;

public class OrderNotCancelableException extends RuntimeException {

    public OrderNotCancelableException(Long orderId) {
        super("결제 대기 중인 주문만 취소할 수 있습니다. orderId=" + orderId);
    }
}
