package com.traditionalmarket.backend.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

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
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentApproval;
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentsClient;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class PaymentApiTest {

    private static final String ADMIN_EMAIL = "admin@traditional-market.local";
    private static final String PAYMENT_KEY = "test-payment-key-123";

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
        OffsetDateTime approvedAt = OffsetDateTime.parse("2026-09-18T16:30:00+09:00");
        given(tossPaymentsClient.confirm(PAYMENT_KEY, orderNumber, 17_000L))
                .willReturn(new TossPaymentApproval(
                        PAYMENT_KEY,
                        orderNumber,
                        "DONE",
                        "카드",
                        17_000L,
                        approvedAt,
                        "https://dashboard.tosspayments.com/receipt/test"
                ));

        mockMvc.perform(post("/api/payments/toss/confirm")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest(orderNumber, 17_000L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.paymentKey").value(PAYMENT_KEY))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.method").value("카드"))
                .andExpect(jsonPath("$.amount").value(17000))
                .andExpect(jsonPath("$.receiptUrl").isNotEmpty());

        assertEquals("PAID", findOrderStatus(orderNumber));
        assertEquals(1, countPayments(orderNumber));
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
        given(tossPaymentsClient.confirm(eq(PAYMENT_KEY), eq(orderNumber), anyLong()))
                .willThrow(new PaymentApprovalException("REJECT_CARD_PAYMENT", "카드 승인이 거절되었습니다."));

        mockMvc.perform(post("/api/payments/toss/confirm")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest(orderNumber, 17_000L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_APPROVAL_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("REJECT_CARD_PAYMENT")));

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

    private String createPendingOrder() throws Exception {
        Long productId = jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE name = '새콤달콤 사과 1kg'",
                Long.class
        );
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

    private String findOrderStatus(String orderNumber) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE order_number = ?",
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
}
