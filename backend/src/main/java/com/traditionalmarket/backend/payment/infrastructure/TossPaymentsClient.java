package com.traditionalmarket.backend.payment.infrastructure;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.traditionalmarket.backend.payment.exception.PaymentApprovalException;
import com.traditionalmarket.backend.payment.exception.PaymentCancellationException;
import com.traditionalmarket.backend.payment.exception.PaymentGatewayUnavailableException;
import com.traditionalmarket.backend.payment.exception.PaymentLookupException;
import com.traditionalmarket.backend.payment.exception.PaymentNotConfiguredException;

@Component
public class TossPaymentsClient {

    private final RestClient restClient;
    private final String secretKey;

    @Autowired
    public TossPaymentsClient(
            @Value("${app.toss-payments.secret-key:}") String secretKey,
            @Value("${app.toss-payments.base-url:https://api.tosspayments.com}") String baseUrl
    ) {
        this(defaultBuilder(), secretKey, baseUrl);
    }

    public TossPaymentsClient(
            RestClient.Builder restClientBuilder,
            String secretKey,
            String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.secretKey = secretKey == null ? "" : secretKey.trim();
    }

    public TossPaymentSnapshot confirm(
            String paymentKey,
            String orderId,
            long amount,
            String idempotencyKey
    ) {
        validateTestSecretKey();

        try {
            TossPaymentResponse response = restClient.post()
                    .uri("/v1/payments/confirm")
                    .headers(headers -> {
                        headers.setBasicAuth(secretKey, "");
                        headers.set("Idempotency-Key", idempotencyKey);
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TossConfirmRequest(paymentKey, orderId, amount))
                    .retrieve()
                    .body(TossPaymentResponse.class);

            return requireResponse(response);
        } catch (RestClientResponseException exception) {
            TossErrorResponse error = readError(
                    exception,
                    "토스페이먼츠 결제 승인이 거절되었습니다."
            );
            throw new PaymentApprovalException(error.code(), error.message());
        } catch (ResourceAccessException exception) {
            throw new PaymentGatewayUnavailableException();
        }
    }

    public TossPaymentSnapshot cancel(
            String paymentKey,
            String cancelReason,
            String idempotencyKey
    ) {
        validateTestSecretKey();

        try {
            TossPaymentResponse response = restClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .headers(headers -> {
                        headers.setBasicAuth(secretKey, "");
                        headers.set("Idempotency-Key", idempotencyKey);
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TossCancelRequest(cancelReason))
                    .retrieve()
                    .body(TossPaymentResponse.class);

            return requireResponse(response);
        } catch (RestClientResponseException exception) {
            TossErrorResponse error = readError(
                    exception,
                    "토스페이먼츠 결제 취소가 거절되었습니다."
            );
            throw new PaymentCancellationException(error.code(), error.message());
        } catch (ResourceAccessException exception) {
            throw new PaymentGatewayUnavailableException();
        }
    }

    public TossPaymentSnapshot getPayment(String paymentKey) {
        validateTestSecretKey();

        try {
            TossPaymentResponse response = restClient.get()
                    .uri("/v1/payments/{paymentKey}", paymentKey)
                    .headers(headers -> headers.setBasicAuth(secretKey, ""))
                    .retrieve()
                    .body(TossPaymentResponse.class);

            return requireResponse(response);
        } catch (RestClientResponseException exception) {
            TossErrorResponse error = readError(
                    exception,
                    "토스페이먼츠 결제 상태를 조회하지 못했습니다."
            );
            throw new PaymentLookupException(error.code(), error.message());
        } catch (ResourceAccessException exception) {
            throw new PaymentGatewayUnavailableException();
        }
    }

    private void validateTestSecretKey() {
        if (!secretKey.startsWith("test_")) {
            throw new PaymentNotConfiguredException();
        }
    }

    private static RestClient.Builder defaultBuilder() {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(7));
        return RestClient.builder().requestFactory(requestFactory);
    }

    private TossErrorResponse readError(
            RestClientResponseException exception,
            String fallbackMessage
    ) {
        try {
            TossErrorResponse error = exception.getResponseBodyAs(TossErrorResponse.class);
            if (error != null) {
                return error;
            }
        } catch (RuntimeException ignored) {
            // 토스 오류 본문을 변환할 수 없으면 HTTP 상태 기반 기본 오류를 사용합니다.
        }
        return new TossErrorResponse(
                "HTTP_" + exception.getStatusCode().value(),
                fallbackMessage
        );
    }

    private TossPaymentSnapshot requireResponse(TossPaymentResponse response) {
        if (response == null) {
            throw new PaymentGatewayUnavailableException();
        }
        return response.toSnapshot();
    }

    private record TossConfirmRequest(String paymentKey, String orderId, long amount) {
    }

    private record TossCancelRequest(String cancelReason) {
    }

    private record TossErrorResponse(String code, String message) {
    }

    private record TossReceipt(String url) {
    }

    private record TossCancelResponse(
            long cancelAmount,
            String cancelReason,
            long refundableAmount,
            java.time.OffsetDateTime canceledAt,
            String transactionKey,
            String cancelStatus
    ) {
        private TossCancellationSnapshot toSnapshot() {
            return new TossCancellationSnapshot(
                    cancelAmount,
                    cancelReason,
                    refundableAmount,
                    canceledAt,
                    transactionKey,
                    cancelStatus
            );
        }
    }

    private record TossPaymentResponse(
            String paymentKey,
            String orderId,
            String status,
            String method,
            long totalAmount,
            long balanceAmount,
            java.time.OffsetDateTime approvedAt,
            TossReceipt receipt,
            List<TossCancelResponse> cancels
    ) {
        private TossPaymentSnapshot toSnapshot() {
            List<TossCancellationSnapshot> cancellationSnapshots = cancels == null
                    ? List.of()
                    : cancels.stream().map(TossCancelResponse::toSnapshot).toList();
            return new TossPaymentSnapshot(
                    paymentKey,
                    orderId,
                    status,
                    method,
                    totalAmount,
                    balanceAmount,
                    approvedAt,
                    receipt == null ? null : receipt.url(),
                    cancellationSnapshots
            );
        }
    }
}
