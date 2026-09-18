package com.traditionalmarket.backend.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("local")
class OrderStockConcurrencyTest {

    private static final String USER_A = "order-a@example.com";
    private static final String USER_B = "order-b@example.com";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long productId;
    private int originalStock;
    private String originalStatus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        productId = jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE name = '새콤달콤 사과 1kg'",
                Long.class
        );
        originalStock = jdbcTemplate.queryForObject(
                "SELECT stock_quantity FROM products WHERE id = ?",
                Integer.class,
                productId
        );
        originalStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM products WHERE id = ?",
                String.class,
                productId
        );

        deleteTestData();
        jdbcTemplate.update(
                "UPDATE products SET stock_quantity = 1, status = 'ON_SALE' WHERE id = ?",
                productId
        );
        createUserAndCart(USER_A, "동시주문A");
        createUserAndCart(USER_B, "동시주문B");
    }

    @AfterEach
    void tearDown() {
        deleteTestData();
        jdbcTemplate.update(
                "UPDATE products SET stock_quantity = ?, status = ? WHERE id = ?",
                originalStock,
                originalStatus,
                productId
        );
    }

    @Test
    void onlyOneOrderSucceedsForTheLastStock() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Integer> first = executor.submit(() -> order(USER_A, ready, start));
            Future<Integer> second = executor.submit(() -> order(USER_B, ready, start));

            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            List<Integer> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertEquals(1, results.stream().filter(status -> status == 201).count());
            assertEquals(1, results.stream().filter(status -> status == 409).count());
            assertEquals(1, count("SELECT COUNT(*) FROM orders WHERE user_id IN (SELECT id FROM users WHERE email IN (?, ?))", USER_A, USER_B));
            assertEquals(0, findStock());
            assertEquals(1, count("SELECT COUNT(*) FROM cart_items ci JOIN carts c ON c.id = ci.cart_id JOIN users u ON u.id = c.user_id WHERE u.email IN (?, ?)", USER_A, USER_B));
        } finally {
            executor.shutdownNow();
        }
    }

    private int order(String email, CountDownLatch ready, CountDownLatch start) throws Exception {
        try {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return mockMvc.perform(post("/api/orders")
                            .with(user(email).roles("USER"))
                            .with(csrf()))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        }
    }

    private void createUserAndCart(String email, String name) {
        jdbcTemplate.update(
                "INSERT INTO users (email, password_hash, name, role, created_at, updated_at) VALUES (?, 'test-hash', ?, 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                email,
                name
        );
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
        );
        jdbcTemplate.update(
                "INSERT INTO carts (user_id, created_at, updated_at) VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                userId
        );
        Long cartId = jdbcTemplate.queryForObject(
                "SELECT id FROM carts WHERE user_id = ?",
                Long.class,
                userId
        );
        jdbcTemplate.update(
                "INSERT INTO cart_items (cart_id, product_id, quantity, created_at, updated_at) VALUES (?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                cartId,
                productId
        );
    }

    private int findStock() {
        return jdbcTemplate.queryForObject(
                "SELECT stock_quantity FROM products WHERE id = ?",
                Integer.class,
                productId
        );
    }

    private int count(String sql, Object... parameters) {
        return jdbcTemplate.queryForObject(sql, Integer.class, parameters);
    }

    private void deleteTestData() {
        jdbcTemplate.update("DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE user_id IN (SELECT id FROM users WHERE email IN (?, ?)))", USER_A, USER_B);
        jdbcTemplate.update("DELETE FROM orders WHERE user_id IN (SELECT id FROM users WHERE email IN (?, ?))", USER_A, USER_B);
        jdbcTemplate.update("DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id IN (SELECT id FROM users WHERE email IN (?, ?)))", USER_A, USER_B);
        jdbcTemplate.update("DELETE FROM carts WHERE user_id IN (SELECT id FROM users WHERE email IN (?, ?))", USER_A, USER_B);
        jdbcTemplate.update("DELETE FROM users WHERE email IN (?, ?)", USER_A, USER_B);
    }
}
