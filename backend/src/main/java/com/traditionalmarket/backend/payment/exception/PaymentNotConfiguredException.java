package com.traditionalmarket.backend.payment.exception;

public class PaymentNotConfiguredException extends RuntimeException {

    public PaymentNotConfiguredException() {
        super("토스페이먼츠 테스트 시크릿 키가 설정되지 않았습니다.");
    }
}
