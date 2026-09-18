package com.traditionalmarket.backend.payment.domain;

import java.time.LocalDateTime;

import com.traditionalmarket.backend.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "toss_webhook_events",
        indexes = {
                @Index(name = "idx_toss_webhook_events_payment", columnList = "payment_key"),
                @Index(name = "idx_toss_webhook_events_processed", columnList = "processed_at")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_toss_webhook_events_transmission",
                columnNames = "transmission_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TossWebhookEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String transmissionId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(length = 200)
    private String paymentKey;

    @Column(nullable = false, length = 64)
    private String payloadHash;

    @Column(nullable = false, length = 30)
    private String result;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    private TossWebhookEvent(
            String transmissionId,
            String eventType,
            String paymentKey,
            String payloadHash,
            String result,
            LocalDateTime processedAt
    ) {
        this.transmissionId = requireText(transmissionId, 100, "웹훅 전송 ID");
        this.eventType = requireText(eventType, 50, "웹훅 이벤트 유형");
        this.paymentKey = normalizeNullable(paymentKey);
        this.payloadHash = requireText(payloadHash, 64, "웹훅 본문 해시");
        this.result = requireText(result, 30, "웹훅 처리 결과");
        if (processedAt == null) {
            throw new IllegalArgumentException("웹훅 처리일시는 필수입니다.");
        }
        this.processedAt = processedAt;
    }

    public static TossWebhookEvent processed(
            String transmissionId,
            String eventType,
            String paymentKey,
            String payloadHash,
            String result,
            LocalDateTime processedAt
    ) {
        return new TossWebhookEvent(
                transmissionId,
                eventType,
                paymentKey,
                payloadHash,
                result,
                processedAt
        );
    }

    private static String requireText(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "이(가) 올바르지 않습니다.");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
