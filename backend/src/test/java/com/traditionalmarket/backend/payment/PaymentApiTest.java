package com.traditionalmarket.backend.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.traditionalmarket.backend.payment.exception.PaymentApprovalException;
import com.traditionalmarket.backend.payment.infrastructure.TossCancellationSnapshot;
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentSnapshot;
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentsClient;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class PaymentApiTest {

    private static final String ADMIN_EMAIL = "admin@traditional-market.local";
    private static final String PAYMENT_KEY = "test-payment-key-123";
    private static final OffsetDateTime APPROVED_AT =
            OffsetDateTime.parse("2026-09-18T16:30:00+09:00");
    private static final OffsetDateTime CANCELED_AT =
            OffsetDateTime.parse("2026-09-18T17:00:00+09:00");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private TossPaymentsClient tossPaymentsClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void confirmsPaymentAndMarksOrderPaid() throws Exception {
        String orderNumber = createPendingOrder();
        givenConfirmation(orderNumber);

        confirm(orderNumber)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.paymentKey").value(PAYMENT_KEY))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.method").value("카드"))
                .andExpect(jsonPath("$.amount").value(17000))
                .andExpect(jsonPath("$.balanceAmount").value(17000));

        assertEquals("PAID", findOrderStatus(orderNumber));
        assertEquals(1, countPayments(orderNumber));
    }

    @Test
    void repeatedConfirmationReturnsStoredResultWithoutCallingTossTwice() throws Exception {
        String orderNumber = createPendingOrder();
        givenConfirmation(orderNumber);

        confirm(orderNumber).andExpect(status().isOk());
        confirm(orderNumber)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").isNumber())
                .andExpect(jsonPath("$.status").value("DONE"));

        verify(tossPaymentsClient, times(1)).confirm(
                eq(PAYMENT_KEY),
                eq(orderNumber),
                eq(17_000L),
                anyString()
        );
        assertEquals(1, countPayments(orderNumber));
    }

    @Test
    void cancelsPaidOrderRestoresStockAndHandlesDuplicateRequest() throws Exception {
        Long productId = findProductId();
        int stockBefore = findStock(productId);
        String orderNumber = createPendingOrder();
        long orderId = findOrderId(orderNumber);
        givenConfirmation(orderNumber);
        confirm(orderNumber).andExpect(status().isOk());
        given(tossPaymentsClient.cancel(eq(PAYMENT_KEY), eq("구매자 요청"), anyString()))
                .willReturn(canceledSnapshot(orderNumber));

        cancel(orderId, "구매자 요청")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.balanceAmount").value(0))
                .andExpect(jsonPath("$.canceledAmount").value(17000))
                .andExpect(jsonPath("$.transactionKey").value("cancel-transaction-1"));

        assertEquals("CANCELED", findOrderStatus(orderNumber));
        assertEquals("CANCELED", findPaymentStatus(orderNumber));
        assertEquals(1, countCancellations(orderNumber));
        assertEquals(stockBefore, findStock(productId));

        cancel(orderId, "다시 요청")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionKey").value("cancel-transaction-1"));
        verify(tossPaymentsClient, times(1)).cancel(
                eq(PAYMENT_KEY),
                eq("구매자 요청"),
                anyString()
        );
        assertEquals(stockBefore, findStock(productId));
    }

    @Test
    void verifiesWebhookWithTossLookupAndDeduplicatesTransmission() throws Exception {
        Long productId = findProductId();
        int stockBefore = findStock(productId);
        String orderNumber = createPendingOrder();
        givenConfirmation(orderNumber);
        confirm(orderNumber).andExpect(status().isOk());
        given(tossPaymentsClient.getPayment(PAYMENT_KEY))
                .willReturn(canceledSnapshot(orderNumber));

        webhook("transmission-123", orderNumber)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("APPLIED"))
                .andExpect(jsonPath("$.duplicate").value(false));
        webhook("transmission-123", orderNumber)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("DUPLICATE"))
                .andExpect(jsonPath("$.duplicate").value(true));

        verify(tossPaymentsClient, times(1)).getPayment(PAYMENT_KEY);
        assertEquals("CANCELED", findOrderStatus(orderNumber));
        assertEquals(1, countWebhookEvents("transmission-123"));
        assertEquals(stockBefore, findStock(productId));
    }

    @Test
    void rejectsTamperedAmountBeforeCallingToss() throws Exception {
        String orderNumber = createPendingOrder();

        mockMvc.perform(post("/api/payments/toss/confirm")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest(orderNumber, 1L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_AMOUNT_MISMATCH"));

        verifyNoInteractions(tossPaymentsClient);
        assertEquals("PENDING_PAYMENT", findOrderStatus(orderNumber));
    }

    @Test
    void returnsProviderApprovalFailureWithoutMarkingOrderPaid() throws Exception {
        String orderNumber = createPendingOrder();
        given(tossPaymentsClient.confirm(
                eq(PAYMENT_KEY),
                eq(orderNumber),
                anyLong(),
                anyString()
        )).willThrow(new PaymentApprovalException(
                "REJECT_CARD_PAYMENT",
                "카드 승인이 거절되었습니다."
        ));

        confirm(orderNumber)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_APPROVAL_FAILED"));

        assertEquals("PENDING_PAYMENT", findOrderStatus(orderNumber));
        assertEquals(0, countPayments(orderNumber));
    }

    @Test
    void validatesAuthenticationCsrfAndRequestBody() throws Exception {
        mockMvc.perform(post("/api/payments/toss/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest("ORD-UNKNOWN", 10_000L)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/payments/toss/confirm")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest("ORD-UNKNOWN", 10_000L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));

        mockMvc.perform(post("/api/payments/toss/confirm")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentKey":"", "orderId":"한글 주문", "amount":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private void givenConfirmation(String orderNumber) {
        given(tossPaymentsClient.confirm(
                eq(PAYMENT_KEY),
                eq(orderNumber),
                eq(17_000L),
                anyString()
        )).willReturn(doneSnapshot(orderNumber));
    }

    private org.springframework.test.web.servlet.ResultActions confirm(String orderNumber)
            throws Exception {
        return mockMvc.perform(post("/api/payments/toss/confirm")
                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmRequest(orderNumber, 17_000L)));
    }

    private org.springframework.test.web.servlet.ResultActions cancel(long orderId, String reason)
            throws Exception {
        return mockMvc.perform(post("/api/payments/toss/orders/{orderId}/cancel", orderId)
                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"cancelReason":"%s"}
                        """.formatted(reason)));
    }

    private org.springframework.test.web.servlet.ResultActions webhook(
            String transmissionId,
            String orderNumber
    ) throws Exception {
        return mockMvc.perform(post("/api/payments/toss/webhook")
                .header("tosspayments-webhook-transmission-id", transmissionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "eventType":"PAYMENT_STATUS_CHANGED",
                          "createdAt":"2026-09-18T17:00:00+09:00",
                          "data":{
                            "paymentKey":"%s",
                            "orderId":"%s",
                            "status":"CANCELED",
                            "totalAmount":17000
                          }
                        }
                        """.formatted(PAYMENT_KEY, orderNumber)));
    }

    private TossPaymentSnapshot doneSnapshot(String orderNumber) {
        return new TossPaymentSnapshot(
                PAYMENT_KEY,
                orderNumber,
                "DONE",
                "카드",
                17_000L,
                17_000L,
                APPROVED_AT,
                "https://dashboard.tosspayments.com/receipt/test",
                List.of()
        );
    }

    private TossPaymentSnapshot canceledSnapshot(String orderNumber) {
        return new TossPaymentSnapshot(
                PAYMENT_KEY,
                orderNumber,
                "CANCELED",
                "카드",
                17_000L,
                0L,
                APPROVED_AT,
                "https://dashboard.tosspayments.com/receipt/test",
                List.of(new TossCancellationSnapshot(
                        17_000L,
                        "구매자 요청",
                        0L,
                        CANCELED_AT,
                        "cancel-transaction-1",
                        "DONE"
                ))
        );
    }

    private String createPendingOrder() throws Exception {
        Long productId = findProductId();
        mockMvc.perform(post("/api/cart/items")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d, "quantity": 2}
                                """.formatted(productId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/orders")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isCreated());
        return jdbcTemplate.queryForObject(
                "SELECT order_number FROM orders ORDER BY id DESC LIMIT 1",
                String.class
        );
    }

    private String confirmRequest(String orderNumber, long amount) {
        return """
                {"paymentKey":"%s", "orderId":"%s", "amount":%d}
                """.formatted(PAYMENT_KEY, orderNumber, amount);
    }

    private Long findProductId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE name = '새콤달콤 사과 1kg'",
                Long.class
        );
    }

    private int findStock(Long productId) {
        return jdbcTemplate.queryForObject(
                "SELECT stock_quantity FROM products WHERE id = ?",
                Integer.class,
                productId
        );
    }

    private long findOrderId(String orderNumber) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM orders WHERE order_number = ?",
                Long.class,
                orderNumber
        );
    }

    private String findOrderStatus(String orderNumber) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE order_number = ?",
                String.class,
                orderNumber
        );
    }

    private String findPaymentStatus(String orderNumber) {
        return jdbcTemplate.queryForObject(
                "SELECT p.status FROM payments p JOIN orders o ON o.id = p.order_id WHERE o.order_number = ?",
                String.class,
                orderNumber
        );
    }

    private int countPayments(String orderNumber) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments p JOIN orders o ON o.id = p.order_id WHERE o.order_number = ?",
                Integer.class,
                orderNumber
        );
    }

    private int countCancellations(String orderNumber) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM payment_cancellations pc
                JOIN payments p ON p.id = pc.payment_id
                JOIN orders o ON o.id = p.order_id
                WHERE o.order_number = ?
                """,
                Integer.class,
                orderNumber
        );
    }

    private int countWebhookEvents(String transmissionId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM toss_webhook_events WHERE transmission_id = ?",
                Integer.class,
                transmissionId
        );
    }
}
