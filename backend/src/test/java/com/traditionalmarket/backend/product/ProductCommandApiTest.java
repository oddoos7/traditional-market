package com.traditionalmarket.backend.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
class ProductCommandApiTest {

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
    void createsProductAsAdmin() throws Exception {
        Long storeId = findStoreId("행복청과");

        mockMvc.perform(post("/api/stores/{storeId}/products", storeId)
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("달콤한 배 1kg", "11000.00", 25, "ON_SALE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.storeId").value(storeId))
                .andExpect(jsonPath("$.storeName").value("행복청과"))
                .andExpect(jsonPath("$.name").value("달콤한 배 1kg"))
                .andExpect(jsonPath("$.price").value(11000.00))
                .andExpect(jsonPath("$.stockQuantity").value(25))
                .andExpect(jsonPath("$.status").value("ON_SALE"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE store_id = ? AND name = ?",
                Integer.class,
                storeId,
                "달콤한 배 1kg"
        );
        assertEquals(1, count);
    }

    @Test
    void updatesProductAsAdminIncludingHiddenStatus() throws Exception {
        Long storeId = findStoreId("행복청과");
        Long productId = findProductId("새콤달콤 사과 1kg");

        mockMvc.perform(put("/api/stores/{storeId}/products/{productId}", storeId, productId)
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("사과 선물세트 2kg", "25000.00", 5, "HIDDEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("사과 선물세트 2kg"))
                .andExpect(jsonPath("$.price").value(25000.00))
                .andExpect(jsonPath("$.stockQuantity").value(5))
                .andExpect(jsonPath("$.status").value("HIDDEN"));
    }

    @Test
    void rejectsProductCreateFromRegularUser() throws Exception {
        Long storeId = findStoreId("행복청과");

        mockMvc.perform(post("/api/stores/{storeId}/products", storeId)
                        .with(user("member@example.com").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("일반회원 상품", "1000.00", 1, "ON_SALE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void requiresAuthenticationWhenCreatingProduct() throws Exception {
        Long storeId = findStoreId("행복청과");

        mockMvc.perform(post("/api/stores/{storeId}/products", storeId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("비로그인 상품", "1000.00", 1, "ON_SALE")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsInvalidPriceAndStockQuantity() throws Exception {
        Long storeId = findStoreId("행복청과");

        mockMvc.perform(post("/api/stores/{storeId}/products", storeId)
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("잘못된 상품", "0.00", -1, "ON_SALE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void returnsNotFoundWhenCreatingProductForMissingStore() throws Exception {
        mockMvc.perform(post("/api/stores/{storeId}/products", 999_999L)
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("없는 점포 상품", "1000.00", 1, "ON_SALE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STORE_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenUpdatingProductFromAnotherStore() throws Exception {
        Long anotherStoreId = findStoreId("중앙반찬");
        Long productId = findProductId("새콤달콤 사과 1kg");

        mockMvc.perform(put(
                        "/api/stores/{storeId}/products/{productId}",
                        anotherStoreId,
                        productId
                )
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("잘못된 수정", "1000.00", 1, "ON_SALE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    private Long findStoreId(String storeName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM stores WHERE name = ?",
                Long.class,
                storeName
        );
    }

    private Long findProductId(String productName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE name = ?",
                Long.class,
                productName
        );
    }

    private String validRequest(String name, String price, int stockQuantity, String status) {
        return """
                {
                  "name": "%s",
                  "description": "전통시장 점포에서 정성껏 준비한 상품입니다.",
                  "price": %s,
                  "stockQuantity": %d,
                  "imageUrl": null,
                  "status": "%s"
                }
                """.formatted(name, price, stockQuantity, status);
    }
}
