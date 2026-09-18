package com.traditionalmarket.backend.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.traditionalmarket.backend.payment.exception.PaymentApprovalException;
import com.traditionalmarket.backend.payment.exception.PaymentNotConfiguredException;
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentSnapshot;
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentsClient;

class TossPaymentsClientTest {

    private static final String BASE_URL = "https://api.tosspayments.com";
    private static final String SECRET_KEY = "test_sk_sample";
    private static final String AUTHORIZATION = "Basic " + Base64.getEncoder().encodeToString(
            (SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8)
    );

    @Test
    void sendsBasicAuthAndIdempotencyKeyWhenConfirming() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsClient client = new TossPaymentsClient(builder, SECRET_KEY, BASE_URL);

        server.expect(requestTo(BASE_URL + "/v1/payments/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andExpect(header("Idempotency-Key", "confirm-idempotency"))
                .andExpect(content().json("""
                        {"paymentKey":"pay-key", "orderId":"ORD-123456", "amount":17000}
                        """))
                .andRespond(withSuccess(doneResponse(), MediaType.APPLICATION_JSON));

        TossPaymentSnapshot payment = client.confirm(
                "pay-key",
                "ORD-123456",
                17_000L,
                "confirm-idempotency"
        );

        assertEquals("DONE", payment.status());
        assertEquals(17_000L, payment.balanceAmount());
        server.verify();
    }

    @Test
    void cancelsWithIdempotencyKeyAndParsesCancellation() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsClient client = new TossPaymentsClient(builder, SECRET_KEY, BASE_URL);

        server.expect(requestTo(BASE_URL + "/v1/payments/pay-key/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andExpect(header("Idempotency-Key", "cancel-idempotency"))
                .andExpect(content().json("""
                        {"cancelReason":"구매자 요청"}
                        """))
                .andRespond(withSuccess(canceledResponse(), MediaType.APPLICATION_JSON));

        TossPaymentSnapshot payment = client.cancel(
                "pay-key",
                "구매자 요청",
                "cancel-idempotency"
        );

        assertEquals("CANCELED", payment.status());
        assertEquals(0L, payment.balanceAmount());
        assertEquals("cancel-transaction-1", payment.cancellations().get(0).transactionKey());
        server.verify();
    }

    @Test
    void looksUpPaymentForWebhookVerification() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsClient client = new TossPaymentsClient(builder, SECRET_KEY, BASE_URL);

        server.expect(requestTo(BASE_URL + "/v1/payments/pay-key"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andRespond(withSuccess(canceledResponse(), MediaType.APPLICATION_JSON));

        TossPaymentSnapshot payment = client.getPayment("pay-key");

        assertEquals("CANCELED", payment.status());
        assertEquals(1, payment.cancellations().size());
        server.verify();
    }

    @Test
    void mapsTossConfirmationErrorResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsClient client = new TossPaymentsClient(builder, SECRET_KEY, BASE_URL);
        server.expect(requestTo(BASE_URL + "/v1/payments/confirm"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"code":"REJECT_CARD_PAYMENT", "message":"카드 승인이 거절되었습니다."}
                                """));

        PaymentApprovalException exception = assertThrows(
                PaymentApprovalException.class,
                () -> client.confirm("pay-key", "ORD-123456", 17_000L, "confirm-key")
        );

        assertEquals("REJECT_CARD_PAYMENT", exception.getProviderCode());
        server.verify();
    }

    @Test
    void refusesMissingOrLiveSecretKey() {
        TossPaymentsClient client = new TossPaymentsClient(
                RestClient.builder(),
                "live_sk_not_allowed",
                BASE_URL
        );

        assertThrows(
                PaymentNotConfiguredException.class,
                () -> client.confirm("pay-key", "ORD-123456", 17_000L, "confirm-key")
        );
    }

    private String doneResponse() {
        return """
                {
                  "paymentKey":"pay-key",
                  "orderId":"ORD-123456",
                  "status":"DONE",
                  "method":"카드",
                  "totalAmount":17000,
                  "balanceAmount":17000,
                  "approvedAt":"2026-09-18T16:30:00+09:00",
                  "receipt":{"url":"https://example.com/receipt"},
                  "cancels":null
                }
                """;
    }

    private String canceledResponse() {
        return """
                {
                  "paymentKey":"pay-key",
                  "orderId":"ORD-123456",
                  "status":"CANCELED",
                  "method":"카드",
                  "totalAmount":17000,
                  "balanceAmount":0,
                  "approvedAt":"2026-09-18T16:30:00+09:00",
                  "receipt":{"url":"https://example.com/receipt"},
                  "cancels":[{
                    "cancelAmount":17000,
                    "cancelReason":"구매자 요청",
                    "refundableAmount":0,
                    "canceledAt":"2026-09-18T17:00:00+09:00",
                    "transactionKey":"cancel-transaction-1",
                    "cancelStatus":"DONE"
                  }]
                }
                """;
    }
}
