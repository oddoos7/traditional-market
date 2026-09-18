package com.traditionalmarket.backend.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.traditionalmarket.backend.common.entity.BaseTimeEntity;
import com.traditionalmarket.backend.order.domain.PurchaseOrder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_status", columnList = "status"),
                @Index(name = "idx_payments_approved_at", columnList = "approved_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payments_order", columnNames = "order_id"),
                @UniqueConstraint(name = "uk_payments_payment_key", columnNames = "payment_key"),
                @UniqueConstraint(
                        name = "uk_payments_confirmation_idempotency",
                        columnNames = "confirmation_idempotency_key"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_order")
    )
    private PurchaseOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(nullable = false, length = 200)
    private String paymentKey;

    @Column(length = 80)
    private String confirmationIdempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(length = 50)
    private String method;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceAmount;

    @Column(nullable = false)
    private LocalDateTime approvedAt;

    private LocalDateTime canceledAt;

    @Column(length = 500)
    private String receiptUrl;

    private Payment(
            PurchaseOrder order,
            String paymentKey,
            String confirmationIdempotencyKey,
            String method,
            BigDecimal amount,
            LocalDateTime approvedAt,
            String receiptUrl
    ) {
        if (order == null || approvedAt == null) {
            throw new IllegalArgumentException("결제 주문과 승인일시는 필수입니다.");
        }
        if (paymentKey == null || paymentKey.isBlank() || paymentKey.length() > 200) {
            throw new IllegalArgumentException("결제 키가 올바르지 않습니다.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("결제 금액이 올바르지 않습니다.");
        }
        this.order = order;
        this.provider = PaymentProvider.TOSS;
        this.paymentKey = paymentKey;
        this.confirmationIdempotencyKey = requireIdempotencyKey(confirmationIdempotencyKey);
        this.status = PaymentStatus.DONE;
        this.method = normalizeNullable(method);
        this.amount = amount.setScale(2);
        this.balanceAmount = this.amount;
        this.approvedAt = approvedAt;
        this.receiptUrl = normalizeNullable(receiptUrl);
    }

    public static Payment approved(
            PurchaseOrder order,
            String paymentKey,
            String confirmationIdempotencyKey,
            String method,
            BigDecimal amount,
            LocalDateTime approvedAt,
            String receiptUrl
    ) {
        return new Payment(
                order,
                paymentKey,
                confirmationIdempotencyKey,
                method,
                amount,
                approvedAt,
                receiptUrl
        );
    }

    public void synchronizeStatus(
            PaymentStatus status,
            BigDecimal balanceAmount,
            LocalDateTime canceledAt
    ) {
        if (status != PaymentStatus.DONE
                && status != PaymentStatus.CANCELED
                && status != PaymentStatus.PARTIAL_CANCELED) {
            throw new IllegalArgumentException("저장할 수 없는 결제 상태입니다.");
        }
        if (balanceAmount == null
                || balanceAmount.signum() < 0
                || balanceAmount.compareTo(amount) > 0) {
            throw new IllegalArgumentException("결제 잔액이 올바르지 않습니다.");
        }
        if (status == PaymentStatus.CANCELED && balanceAmount.signum() != 0) {
            throw new IllegalArgumentException("전액 취소된 결제의 잔액은 0원이어야 합니다.");
        }
        this.status = status;
        this.balanceAmount = balanceAmount.setScale(2);
        this.canceledAt = canceledAt;
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.length() > 80) {
            throw new IllegalArgumentException("결제 승인 멱등키가 올바르지 않습니다.");
        }
        return value.trim();
    }
}
