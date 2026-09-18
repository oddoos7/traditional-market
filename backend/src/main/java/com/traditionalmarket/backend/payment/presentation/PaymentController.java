package com.traditionalmarket.backend.payment.presentation;

import java.security.Principal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.traditionalmarket.backend.payment.application.PaymentService;
import com.traditionalmarket.backend.payment.dto.request.TossPaymentConfirmRequest;
import com.traditionalmarket.backend.payment.dto.response.PaymentConfirmResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/toss")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public PaymentConfirmResponse confirm(
            Principal principal,
            @Valid @RequestBody TossPaymentConfirmRequest request
    ) {
        return paymentService.confirmTossPayment(principal.getName(), request);
    }
}
