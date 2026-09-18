package com.traditionalmarket.backend.payment.exception;

public class PaymentOrderNotPayableException extends RuntimeException {

    public PaymentOrderNotPayableException(Long orderId) {
        super("결제 대기 중인 주문만 승인할 수 있습니다. orderId=" + orderId);
    }
}
