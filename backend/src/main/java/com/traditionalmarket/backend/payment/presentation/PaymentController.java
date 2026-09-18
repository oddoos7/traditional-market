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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/toss")
public class PaymentController {

    private final PaymentService paymentService;
    private final TossPaymentWebhookService tossPaymentWebhookService;

    @PostMapping("/confirm")
    public PaymentConfirmResponse confirm(
            Principal principal,
            @Valid @RequestBody TossPaymentConfirmRequest request
    ) {
        return paymentService.confirmTossPayment(principal.getName(), request);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public PaymentCancelResponse cancel(
            Principal principal,
            @PathVariable @Positive Long orderId,
            @Valid @RequestBody TossPaymentCancelRequest request
    ) {
        return paymentService.cancelTossPayment(principal.getName(), orderId, request);
    }

    @PostMapping("/webhook")
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
