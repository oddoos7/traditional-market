package com.traditionalmarket.backend.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TossPaymentConfirmRequest(
        @NotBlank(message = "결제 키는 필수입니다.")
        @Size(max = 200, message = "결제 키는 200자 이하여야 합니다.")
        String paymentKey,

        @NotBlank(message = "주문번호는 필수입니다.")
        @Pattern(
                regexp = "[A-Za-z0-9_-]{6,64}",
                message = "주문번호 형식이 올바르지 않습니다."
        )
        String orderId,

        @Positive(message = "결제 금액은 0보다 커야 합니다.")
        long amount
) {
}
