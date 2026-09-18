package com.traditionalmarket.backend.payment.exception;

public class PaymentAmountMismatchException extends RuntimeException {

    public PaymentAmountMismatchException() {
        super("주문 금액과 결제 승인 금액이 일치하지 않습니다.");
    }
}
