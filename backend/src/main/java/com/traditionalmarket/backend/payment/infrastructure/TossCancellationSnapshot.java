package com.traditionalmarket.backend.payment.infrastructure;

import java.time.OffsetDateTime;

public record TossCancellationSnapshot(
        long cancelAmount,
        String cancelReason,
        long refundableAmount,
        OffsetDateTime canceledAt,
        String transactionKey,
        String cancelStatus
) {
}
