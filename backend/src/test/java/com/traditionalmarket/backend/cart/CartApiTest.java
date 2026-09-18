package com.traditionalmarket.backend.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class CartApiTest {

    private static final String ADMIN_EMAIL = "admin@traditional-market.local";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void addsListsUpdatesAndRemovesCartItem() throws Exception {
        Long productId = findProductId("새콤달콤 사과 1kg");

        mockMvc.perform(post("/api/cart/items")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest(productId, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeName").value("행복청과"))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].available").value(true))
                .andExpect(jsonPath("$.totalItemCount").value(2))
                .andExpect(jsonPath("$.totalAmount").value(17000))
                .andExpect(jsonPath("$.orderable").value(true));

        Long cartItemId = findCartItemId(productId);

        mockMvc.perform(get("/api/cart")
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cartItemId").value(cartItemId));

        mockMvc.perform(put("/api/cart/items/{cartItemId}", cartItemId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.totalItemCount").value(3))
                .andExpect(jsonPath("$.totalAmount").value(25500));

        mockMvc.perform(delete("/api/cart/items/{cartItemId}", cartItemId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalItemCount").value(0))
                .andExpect(jsonPath("$.orderable").value(false));
    }

    @Test
    void addingSameProductCombinesQuantityWithoutDuplicateRows() throws Exception {
        Long productId = findProductId("새콤달콤 사과 1kg");

        addProduct(productId, 2);
        addProduct(productId, 3);

        mockMvc.perform(get("/api/cart")
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(5));

        Integer itemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cart_items WHERE product_id = ?",
                Integer.class,
                productId
        );
        assertEquals(1, itemCount);
    }

    @Test
    void rejectsProductsFromAnotherStoreUntilCartIsCleared() throws Exception {
        Long appleId = findProductId("새콤달콤 사과 1kg");
        Long sideDishId = findProductId("수제 잡채 500g");
        addProduct(appleId, 1);

        mockMvc.perform(post("/api/cart/items")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest(sideDishId, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CART_STORE_MISMATCH"));

        mockMvc.perform(delete("/api/cart")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        mockMvc.perform(post("/api/cart/items")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest(sideDishId, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeName").value("중앙반찬"));
    }

    @Test
    void rejectsSoldOutAndExcessiveQuantity() throws Exception {
        Long soldOutProductId = findProductId("제철 딸기 500g");
        Long appleId = findProductId("새콤달콤 사과 1kg");

        mockMvc.perform(post("/api/cart/items")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest(soldOutProductId, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_AVAILABLE"));

        mockMvc.perform(post("/api/cart/items")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest(appleId, 31)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CART_QUANTITY_EXCEEDED"));
    }

    @Test
    void validatesAuthenticationCsrfAndRequestValues() throws Exception {
        Long productId = findProductId("새콤달콤 사과 1kg");

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(post("/api/cart/items")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest(productId, 1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));

        mockMvc.perform(post("/api/cart/items")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest(productId, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsUnknownProductAndCartItem() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest(999_999L, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        mockMvc.perform(delete("/api/cart/items/{cartItemId}", 999_999L)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));
    }

    private void addProduct(Long productId, int quantity) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest(productId, quantity)))
                .andExpect(status().isCreated());
    }

    private String addRequest(Long productId, int quantity) {
        return """
                {"productId": %d, "quantity": %d}
                """.formatted(productId, quantity);
    }

    private Long findProductId(String productName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE name = ?",
                Long.class,
                productName
        );
    }

    private Long findCartItemId(Long productId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT ci.id
                FROM cart_items ci
                JOIN carts c ON c.id = ci.cart_id
                JOIN users u ON u.id = c.user_id
                WHERE u.email = ? AND ci.product_id = ?
                """,
                Long.class,
                ADMIN_EMAIL,
                productId
        );
    }
}
