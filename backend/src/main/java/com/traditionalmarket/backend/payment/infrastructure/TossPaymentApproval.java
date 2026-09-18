package com.traditionalmarket.backend.payment.infrastructure;

import java.time.OffsetDateTime;

public record TossPaymentApproval(
        String paymentKey,
        String orderId,
        String status,
        String method,
        long totalAmount,
        OffsetDateTime approvedAt,
        String receiptUrl
) {
}
