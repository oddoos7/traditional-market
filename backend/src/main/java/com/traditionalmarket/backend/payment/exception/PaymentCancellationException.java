package com.traditionalmarket.backend.payment.exception;

public class PaymentCancellationException extends RuntimeException {

    private final String providerCode;

    public PaymentCancellationException(String providerCode, String message) {
        super(message == null || message.isBlank() ? "토스페이먼츠 결제 취소가 거절되었습니다." : message);
        this.providerCode = providerCode == null || providerCode.isBlank()
                ? "UNKNOWN"
                : providerCode;
    }

    public String getProviderCode() {
        return providerCode;
    }
}
