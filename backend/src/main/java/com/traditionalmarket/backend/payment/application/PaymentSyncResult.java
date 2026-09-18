package com.traditionalmarket.backend.payment.application;

import com.traditionalmarket.backend.payment.domain.Payment;
import com.traditionalmarket.backend.payment.domain.PaymentCancellation;

public record PaymentSyncResult(
        Payment payment,
        PaymentCancellation latestCancellation,
        String result
) {
}
