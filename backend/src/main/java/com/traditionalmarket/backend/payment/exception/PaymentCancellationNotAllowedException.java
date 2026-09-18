package com.traditionalmarket.backend.payment.exception;

public class PaymentCancellationNotAllowedException extends RuntimeException {

    public PaymentCancellationNotAllowedException(Long orderId) {
        super("취소할 수 없는 결제입니다. orderId=" + orderId);
    }
}
