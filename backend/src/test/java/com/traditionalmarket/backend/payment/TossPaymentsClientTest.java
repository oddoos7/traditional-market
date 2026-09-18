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
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentApproval;
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentsClient;

class TossPaymentsClientTest {

    @Test
    void sendsBasicAuthAndParsesApproval() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsClient client = new TossPaymentsClient(
                builder,
                "test_sk_sample",
                "https://api.tosspayments.com"
        );
        String authorization = "Basic " + Base64.getEncoder().encodeToString(
                "test_sk_sample:".getBytes(StandardCharsets.UTF_8)
        );

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(content().json("""
                        {"paymentKey":"pay-key", "orderId":"ORD-123456", "amount":17000}
                        """))
                .andRespond(withSuccess("""
                        {
                          "paymentKey":"pay-key",
                          "orderId":"ORD-123456",
                          "status":"DONE",
                          "method":"카드",
                          "totalAmount":17000,
                          "approvedAt":"2026-09-18T16:30:00+09:00",
                          "receipt":{"url":"https://example.com/receipt"}
                        }
                        """, MediaType.APPLICATION_JSON));

        TossPaymentApproval approval = client.confirm("pay-key", "ORD-123456", 17_000L);

        assertEquals("DONE", approval.status());
        assertEquals(17_000L, approval.totalAmount());
        assertEquals("https://example.com/receipt", approval.receiptUrl());
        server.verify();
    }

    @Test
    void mapsTossErrorResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsClient client = new TossPaymentsClient(
                builder,
                "test_sk_sample",
                "https://api.tosspayments.com"
        );
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"code":"REJECT_CARD_PAYMENT", "message":"카드 승인이 거절되었습니다."}
                                """));

        PaymentApprovalException exception = assertThrows(
                PaymentApprovalException.class,
                () -> client.confirm("pay-key", "ORD-123456", 17_000L)
        );

        assertEquals("REJECT_CARD_PAYMENT", exception.getProviderCode());
        server.verify();
    }

    @Test
    void refusesMissingOrLiveSecretKey() {
        TossPaymentsClient client = new TossPaymentsClient(
                RestClient.builder(),
                "live_sk_not_allowed",
                "https://api.tosspayments.com"
        );

        assertThrows(
                PaymentNotConfiguredException.class,
                () -> client.confirm("pay-key", "ORD-123456", 17_000L)
        );
    }
}
