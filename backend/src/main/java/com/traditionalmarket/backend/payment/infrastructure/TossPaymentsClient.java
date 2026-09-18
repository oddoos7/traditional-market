package com.traditionalmarket.backend.payment.infrastructure;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.traditionalmarket.backend.payment.exception.PaymentApprovalException;
import com.traditionalmarket.backend.payment.exception.PaymentGatewayUnavailableException;
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

    public TossPaymentApproval confirm(String paymentKey, String orderId, long amount) {
        validateTestSecretKey();

        try {
            TossPaymentResponse response = restClient.post()
                    .uri("/v1/payments/confirm")
                    .headers(headers -> headers.setBasicAuth(secretKey, ""))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TossConfirmRequest(paymentKey, orderId, amount))
                    .retrieve()
                    .body(TossPaymentResponse.class);

            if (response == null) {
                throw new PaymentGatewayUnavailableException();
            }
            return response.toApproval();
        } catch (RestClientResponseException exception) {
            TossErrorResponse error = readError(exception);
            throw new PaymentApprovalException(error.code(), error.message());
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
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(requestFactory);
    }

    private TossErrorResponse readError(RestClientResponseException exception) {
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
                "토스페이먼츠 결제 승인이 거절되었습니다."
        );
    }

    private record TossConfirmRequest(String paymentKey, String orderId, long amount) {
    }

    private record TossErrorResponse(String code, String message) {
    }

    private record TossReceipt(String url) {
    }

    private record TossPaymentResponse(
            String paymentKey,
            String orderId,
            String status,
            String method,
            long totalAmount,
            java.time.OffsetDateTime approvedAt,
            TossReceipt receipt
    ) {
        private TossPaymentApproval toApproval() {
            return new TossPaymentApproval(
                    paymentKey,
                    orderId,
                    status,
                    method,
                    totalAmount,
                    approvedAt,
                    receipt == null ? null : receipt.url()
            );
        }
    }
}
