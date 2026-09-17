package com.traditionalmarket.backend.product;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class ProductQueryApiTest {

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
    void returnsPagedVisibleProductsBelongingToStore() throws Exception {
        Long storeId = findStoreId("행복청과");

        mockMvc.perform(get("/api/stores/{storeId}/products", storeId)
                        .param("size", "1")
                        .param("sort", "NAME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].storeId").value(storeId))
                .andExpect(jsonPath("$.content[0].status").isNotEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void filtersProductsByKeywordAndStatus() throws Exception {
        Long storeId = findStoreId("행복청과");

        mockMvc.perform(get("/api/stores/{storeId}/products", storeId)
                        .param("keyword", "딸기")
                        .param("status", "SOLD_OUT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("제철 딸기 500g"))
                .andExpect(jsonPath("$.content[0].stockQuantity").value(0))
                .andExpect(jsonPath("$.content[0].status").value("SOLD_OUT"));
    }

    @Test
    void sortsProductsByPriceDescendingWithStableTieBreaker() throws Exception {
        Long storeId = findStoreId("중앙반찬");

        mockMvc.perform(get("/api/stores/{storeId}/products", storeId)
                        .param("sort", "PRICE_HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("포기김치 1kg"))
                .andExpect(jsonPath("$.content[1].name").value("수제 잡채 500g"));
    }

    @Test
    void returnsVisibleProductDetailWithStoreAndMarketInformation() throws Exception {
        Long storeId = findStoreId("행복청과");
        Long productId = findProductId("새콤달콤 사과 1kg");

        mockMvc.perform(get("/api/stores/{storeId}/products/{productId}", storeId, productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.storeId").value(storeId))
                .andExpect(jsonPath("$.storeName").value("행복청과"))
                .andExpect(jsonPath("$.marketName").value("우리동네 중앙시장"))
                .andExpect(jsonPath("$.name").value("새콤달콤 사과 1kg"))
                .andExpect(jsonPath("$.price").value(8500.00))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void excludesHiddenProductFromPublicListAndDetail() throws Exception {
        Long storeId = findStoreId("한빛국수");
        Long productId = findProductId("멸치국수 밀키트");

        mockMvc.perform(get("/api/stores/{storeId}/products", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/stores/{storeId}/products/{productId}", storeId, productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenProductBelongsToAnotherStore() throws Exception {
        Long storeId = findStoreId("중앙반찬");
        Long productId = findProductId("새콤달콤 사과 1kg");

        mockMvc.perform(get("/api/stores/{storeId}/products/{productId}", storeId, productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void validatesStoreAndPagingParameters() throws Exception {
        mockMvc.perform(get("/api/stores/{storeId}/products", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STORE_NOT_FOUND"));

        Long storeId = findStoreId("행복청과");
        mockMvc.perform(get("/api/stores/{storeId}/products", storeId)
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
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
}
