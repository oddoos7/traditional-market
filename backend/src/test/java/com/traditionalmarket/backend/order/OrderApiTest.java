package com.traditionalmarket.backend.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class OrderApiTest {

    private static final String ADMIN_EMAIL = "admin@traditional-market.local";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void createsOrderDecreasesStockAndClearsCart() throws Exception {
        Long productId = findProductId("새콤달콤 사과 1kg");
        int stockBefore = findStock(productId);
        addCartItem(productId, 2);

        mockMvc.perform(post("/api/orders")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value(org.hamcrest.Matchers.startsWith("ORD-")))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.storeName").value("행복청과"))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalItemCount").value(2))
                .andExpect(jsonPath("$.totalAmount").value(17000));

        long orderId = findLatestOrderId();
        assertEquals(stockBefore - 2, findStock(productId));
        assertEquals(0, countCartItems());

        mockMvc.perform(get("/api/orders")
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId))
                .andExpect(jsonPath("$[0].totalItemCount").value(2));

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("새콤달콤 사과 1kg"));
    }

    @Test
    void cancellationRestoresStockOnlyOnce() throws Exception {
        Long productId = findProductId("새콤달콤 사과 1kg");
        int stockBefore = findStock(productId);
        addCartItem(productId, 3);
        long orderId = createOrder();

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.canceledAt").isNotEmpty());

        assertEquals(stockBefore, findStock(productId));

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_CANCELABLE"));
        assertEquals(stockBefore, findStock(productId));
    }

    @Test
    void rejectsEmptyCartAndChangedStock() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMPTY_CART"));

        Long productId = findProductId("새콤달콤 사과 1kg");
        addCartItem(productId, 2);
        jdbcTemplate.update("UPDATE products SET stock_quantity = 1 WHERE id = ?", productId);
        entityManager.clear();

        mockMvc.perform(post("/api/orders")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_STOCK_UNAVAILABLE"));

        assertEquals(1, findStock(productId));
        assertEquals(1, countCartItems());
    }

    @Test
    void validatesOwnerAuthenticationCsrfAndPositiveId() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(post("/api/orders")
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));

        mockMvc.perform(get("/api/orders/{orderId}", 999_999L)
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        mockMvc.perform(get("/api/orders/{orderId}", 0)
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private void addCartItem(Long productId, int quantity) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d, "quantity": %d}
                                """.formatted(productId, quantity)))
                .andExpect(status().isCreated());
    }

    private long createOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isCreated());
        return findLatestOrderId();
    }

    private Long findProductId(String productName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE name = ?",
                Long.class,
                productName
        );
    }

    private int findStock(Long productId) {
        return jdbcTemplate.queryForObject(
                "SELECT stock_quantity FROM products WHERE id = ?",
                Integer.class,
                productId
        );
    }

    private int countCartItems() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cart_items", Integer.class);
    }

    private long findLatestOrderId() {
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM orders", Long.class);
    }
}
