package com.traditionalmarket.backend.payment.dto.request;

public record TossWebhookPaymentData(
        String paymentKey,
        String orderId,
        String status,
        Long totalAmount
) {
}
