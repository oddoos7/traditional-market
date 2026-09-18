package com.traditionalmarket.backend.payment.presentation;

import java.security.Principal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.traditionalmarket.backend.payment.application.PaymentService;
import com.traditionalmarket.backend.payment.application.TossPaymentWebhookService;
import com.traditionalmarket.backend.payment.dto.request.TossPaymentCancelRequest;
import com.traditionalmarket.backend.payment.dto.request.TossPaymentConfirmRequest;
import com.traditionalmarket.backend.payment.dto.request.TossWebhookRequest;
import com.traditionalmarket.backend.payment.dto.response.PaymentCancelResponse;
import com.traditionalmarket.backend.payment.dto.response.PaymentConfirmResponse;
import com.traditionalmarket.backend.payment.dto.response.TossWebhookResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/toss")
@Tag(name = "결제", description = "토스페이먼츠 결제 승인, 취소 및 웹훅 API")
public class PaymentController {

    private final PaymentService paymentService;
    private final TossPaymentWebhookService tossPaymentWebhookService;

    @PostMapping("/confirm")
    @Operation(summary = "토스 결제 승인")
    public PaymentConfirmResponse confirm(
            Principal principal,
            @Valid @RequestBody TossPaymentConfirmRequest request
    ) {
        return paymentService.confirmTossPayment(principal.getName(), request);
    }

    @PostMapping("/orders/{orderId}/cancel")
    @Operation(summary = "토스 결제 전액 취소")
    public PaymentCancelResponse cancel(
            Principal principal,
            @PathVariable @Positive Long orderId,
            @Valid @RequestBody TossPaymentCancelRequest request
    ) {
        return paymentService.cancelTossPayment(principal.getName(), orderId, request);
    }

    @PostMapping("/webhook")
    @Operation(summary = "토스 결제 상태 웹훅 수신")
    public TossWebhookResponse webhook(
            @RequestHeader("tosspayments-webhook-transmission-id")
            @NotBlank
            @Size(max = 100)
            String transmissionId,
            @Valid @RequestBody TossWebhookRequest request
    ) {
        return tossPaymentWebhookService.process(transmissionId, request);
    }
}
