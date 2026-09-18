package com.traditionalmarket.backend.payment.exception;

public class PaymentLookupException extends RuntimeException {

    private final String providerCode;

    public PaymentLookupException(String providerCode, String message) {
        super(message == null || message.isBlank() ? "토스페이먼츠 결제 상태를 조회하지 못했습니다." : message);
        this.providerCode = providerCode == null || providerCode.isBlank()
                ? "UNKNOWN"
                : providerCode;
    }

    public String getProviderCode() {
        return providerCode;
    }
}
