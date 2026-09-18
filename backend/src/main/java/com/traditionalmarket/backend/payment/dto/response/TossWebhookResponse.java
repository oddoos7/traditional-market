package com.traditionalmarket.backend.payment.dto.response;

public record TossWebhookResponse(
        String transmissionId,
        String result,
        boolean duplicate
) {
}
