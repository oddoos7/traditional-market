package com.traditionalmarket.backend.payment.infrastructure;

import java.time.OffsetDateTime;
import java.util.List;

public record TossPaymentSnapshot(
        String paymentKey,
        String orderId,
        String status,
        String method,
        long totalAmount,
        long balanceAmount,
        OffsetDateTime approvedAt,
        String receiptUrl,
        List<TossCancellationSnapshot> cancellations
) {
    public TossPaymentSnapshot {
        cancellations = cancellations == null ? List.of() : List.copyOf(cancellations);
    }
}
