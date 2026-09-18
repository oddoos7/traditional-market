package com.traditionalmarket.backend.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.traditionalmarket.backend.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "payment_cancellations",
        indexes = @Index(
                name = "idx_payment_cancellations_payment",
                columnList = "payment_id"
        ),
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_cancellations_transaction",
                        columnNames = "transaction_key"
                ),
                @UniqueConstraint(
                        name = "uk_payment_cancellations_idempotency",
                        columnNames = "idempotency_key"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentCancellation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "payment_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_payment_cancellations_payment")
    )
    private Payment payment;

    @Column(nullable = false, length = 64)
    private String transactionKey;

    @Column(length = 80)
    private String idempotencyKey;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal cancelAmount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal refundableAmount;

    @Column(nullable = false, length = 200)
    private String cancelReason;

    @Column(nullable = false, length = 20)
    private String cancelStatus;

    @Column(nullable = false)
    private LocalDateTime canceledAt;

    private PaymentCancellation(
            Payment payment,
            String transactionKey,
            String idempotencyKey,
            BigDecimal cancelAmount,
            BigDecimal refundableAmount,
            String cancelReason,
            LocalDateTime canceledAt
    ) {
        if (payment == null || canceledAt == null) {
            throw new IllegalArgumentException("결제와 취소일시는 필수입니다.");
        }
        if (transactionKey == null || transactionKey.isBlank() || transactionKey.length() > 64) {
            throw new IllegalArgumentException("취소 거래 키가 올바르지 않습니다.");
        }
        if (cancelAmount == null || cancelAmount.signum() <= 0
                || refundableAmount == null || refundableAmount.signum() < 0) {
            throw new IllegalArgumentException("취소 금액이 올바르지 않습니다.");
        }
        if (cancelReason == null || cancelReason.isBlank() || cancelReason.length() > 200) {
            throw new IllegalArgumentException("취소 사유가 올바르지 않습니다.");
        }
        this.payment = payment;
        this.transactionKey = transactionKey.trim();
        this.idempotencyKey = normalizeNullable(idempotencyKey);
        this.cancelAmount = cancelAmount.setScale(2);
        this.refundableAmount = refundableAmount.setScale(2);
        this.cancelReason = cancelReason.trim();
        this.cancelStatus = "DONE";
        this.canceledAt = canceledAt;
    }

    public static PaymentCancellation completed(
            Payment payment,
            String transactionKey,
            String idempotencyKey,
            BigDecimal cancelAmount,
            BigDecimal refundableAmount,
            String cancelReason,
            LocalDateTime canceledAt
    ) {
        return new PaymentCancellation(
                payment,
                transactionKey,
                idempotencyKey,
                cancelAmount,
                refundableAmount,
                cancelReason,
                canceledAt
        );
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
