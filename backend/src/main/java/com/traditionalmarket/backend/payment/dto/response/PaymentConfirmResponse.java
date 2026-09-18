package com.traditionalmarket.backend.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.traditionalmarket.backend.payment.domain.Payment;
import com.traditionalmarket.backend.payment.domain.PaymentStatus;

public record PaymentConfirmResponse(
        Long paymentId,
        Long orderId,
        String orderNumber,
        String paymentKey,
        PaymentStatus status,
        String method,
        BigDecimal amount,
        BigDecimal balanceAmount,
        LocalDateTime approvedAt,
        LocalDateTime canceledAt,
        String receiptUrl
) {

    public static PaymentConfirmResponse from(Payment payment) {
        return new PaymentConfirmResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getOrder().getOrderNumber(),
                payment.getPaymentKey(),
                payment.getStatus(),
                payment.getMethod(),
                payment.getAmount(),
                payment.getBalanceAmount(),
                payment.getApprovedAt(),
                payment.getCanceledAt(),
                payment.getReceiptUrl()
        );
    }
}
