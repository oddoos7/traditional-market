package com.traditionalmarket.backend.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TossWebhookRequest(
        @NotBlank(message = "웹훅 이벤트 유형은 필수입니다.")
        @Size(max = 50, message = "웹훅 이벤트 유형은 50자 이하여야 합니다.")
        String eventType,

        @NotBlank(message = "웹훅 생성일시는 필수입니다.")
        @Size(max = 50, message = "웹훅 생성일시는 50자 이하여야 합니다.")
        String createdAt,

        TossWebhookPaymentData data
) {
}
