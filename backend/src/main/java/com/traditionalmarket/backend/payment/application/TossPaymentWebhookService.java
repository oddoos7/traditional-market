package com.traditionalmarket.backend.payment.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.traditionalmarket.backend.order.domain.PurchaseOrder;
import com.traditionalmarket.backend.order.repository.PurchaseOrderRepository;
import com.traditionalmarket.backend.payment.domain.Payment;
import com.traditionalmarket.backend.payment.domain.TossWebhookEvent;
import com.traditionalmarket.backend.payment.dto.request.TossWebhookRequest;
import com.traditionalmarket.backend.payment.dto.response.TossWebhookResponse;
import com.traditionalmarket.backend.payment.exception.PaymentVerificationException;
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentSnapshot;
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentsClient;
import com.traditionalmarket.backend.payment.repository.PaymentRepository;
import com.traditionalmarket.backend.payment.repository.TossWebhookEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TossPaymentWebhookService {

    private static final String PAYMENT_STATUS_CHANGED = "PAYMENT_STATUS_CHANGED";

    private final TossWebhookEventRepository webhookEventRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final PaymentStateSynchronizer paymentStateSynchronizer;

    @Transactional
    public TossWebhookResponse process(
            String transmissionId,
            TossWebhookRequest request
    ) {
        String payloadHash = hashPayload(request);
        TossWebhookEvent existingEvent = webhookEventRepository
                .findByTransmissionId(transmissionId)
                .orElse(null);
        if (existingEvent != null) {
            if (!existingEvent.getPayloadHash().equals(payloadHash)) {
                throw new PaymentVerificationException();
            }
            return new TossWebhookResponse(transmissionId, "DUPLICATE", true);
        }
        if (!PAYMENT_STATUS_CHANGED.equals(request.eventType())) {
            saveEvent(transmissionId, request.eventType(), null, payloadHash, "IGNORED");
            return new TossWebhookResponse(transmissionId, "IGNORED", false);
        }

        if (request.data() == null) {
            throw new PaymentVerificationException();
        }

        String paymentKey = normalizeText(request.data().paymentKey());
        if (paymentKey == null || paymentKey.length() > 200) {
            throw new PaymentVerificationException();
        }

        // 일반 결제 웹훅에는 서명이 없으므로 본문을 신뢰하지 않고 토스 조회 API로 검증합니다.
        TossPaymentSnapshot snapshot = tossPaymentsClient.getPayment(paymentKey);
        if (!paymentKey.equals(snapshot.paymentKey())) {
            throw new PaymentVerificationException();
        }

        PurchaseOrder order = purchaseOrderRepository
                .findByOrderNumberForUpdate(snapshot.orderId())
                .orElse(null);
        if (order == null) {
            saveEvent(
                    transmissionId,
                    request.eventType(),
                    paymentKey,
                    payloadHash,
                    "IGNORED"
            );
            return new TossWebhookResponse(transmissionId, "IGNORED", false);
        }

        Payment payment = paymentRepository.findByOrderIdForUpdate(order.getId()).orElse(null);
        PaymentSyncResult syncResult = paymentStateSynchronizer.synchronize(
                order,
                payment,
                snapshot,
                payment == null ? null : payment.getConfirmationIdempotencyKey(),
                null
        );
        saveEvent(
                transmissionId,
                request.eventType(),
                paymentKey,
                payloadHash,
                syncResult.result()
        );
        return new TossWebhookResponse(transmissionId, syncResult.result(), false);
    }

    private void saveEvent(
            String transmissionId,
            String eventType,
            String paymentKey,
            String payloadHash,
            String result
    ) {
        webhookEventRepository.saveAndFlush(TossWebhookEvent.processed(
                transmissionId,
                eventType,
                paymentKey,
                payloadHash,
                result,
                LocalDateTime.now()
        ));
    }

    private String hashPayload(TossWebhookRequest request) {
        try {
            var data = request.data();
            String payloadValue = String.join(
                    "\n",
                    String.valueOf(request.eventType()),
                    String.valueOf(request.createdAt()),
                    String.valueOf(data == null ? null : data.paymentKey()),
                    String.valueOf(data == null ? null : data.orderId()),
                    String.valueOf(data == null ? null : data.status()),
                    String.valueOf(data == null ? null : data.totalAmount())
            );
            byte[] payload = payloadValue.getBytes(StandardCharsets.UTF_8);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(payload);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("웹훅 본문 해시를 생성하지 못했습니다.", exception);
        }
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
