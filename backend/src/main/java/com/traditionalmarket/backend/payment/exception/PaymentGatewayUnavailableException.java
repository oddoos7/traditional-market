package com.traditionalmarket.backend.payment.exception;

public class PaymentGatewayUnavailableException extends RuntimeException {

    public PaymentGatewayUnavailableException() {
        super("결제 승인 서버에 연결하지 못했습니다. 잠시 후 결제 상태를 확인해주세요.");
    }
}
