package com.traditionalmarket.backend.payment.exception;

public class PaymentVerificationException extends RuntimeException {

    public PaymentVerificationException() {
        super("토스페이먼츠 승인 결과와 주문 정보가 일치하지 않습니다.");
    }
}
