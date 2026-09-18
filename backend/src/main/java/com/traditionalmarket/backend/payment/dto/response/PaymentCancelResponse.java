package com.traditionalmarket.backend.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.traditionalmarket.backend.payment.domain.Payment;
import com.traditionalmarket.backend.payment.domain.PaymentCancellation;
import com.traditionalmarket.backend.payment.domain.PaymentStatus;

public record PaymentCancelResponse(
        Long paymentId,
        Long orderId,
        String orderNumber,
        String paymentKey,
        PaymentStatus status,
        BigDecimal amount,
        BigDecimal balanceAmount,
        BigDecimal canceledAmount,
        String cancelReason,
        String transactionKey,
        LocalDateTime canceledAt
) {

    public static PaymentCancelResponse from(
            Payment payment,
            PaymentCancellation cancellation
    ) {
        return new PaymentCancelResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getOrder().getOrderNumber(),
                payment.getPaymentKey(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getBalanceAmount(),
                cancellation.getCancelAmount(),
                cancellation.getCancelReason(),
                cancellation.getTransactionKey(),
                cancellation.getCanceledAt()
        );
    }
}
