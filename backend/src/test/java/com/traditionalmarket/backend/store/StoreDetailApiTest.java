package com.traditionalmarket.backend.store;

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
class StoreDetailApiTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void returnsStoreDetailBelongingToMarket() throws Exception {
        Long marketId = findMarketId("우리동네 중앙시장");
        Long storeId = findStoreId("행복청과");

        mockMvc.perform(get("/api/markets/{marketId}/stores/{storeId}", marketId, storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(storeId))
                .andExpect(jsonPath("$.marketId").value(marketId))
                .andExpect(jsonPath("$.marketName").value("우리동네 중앙시장"))
                .andExpect(jsonPath("$.name").value("행복청과"))
                .andExpect(jsonPath("$.category").value("AGRICULTURAL"))
                .andExpect(jsonPath("$.location").value("가동 12호"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void returnsNotFoundWhenStoreBelongsToAnotherMarket() throws Exception {
        Long centralMarketId = findMarketId("우리동네 중앙시장");
        Long alleyStoreId = findStoreId("푸른떡방");

        mockMvc.perform(get(
                        "/api/markets/{marketId}/stores/{storeId}",
                        centralMarketId,
                        alleyStoreId
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STORE_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenMarketDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/markets/{marketId}/stores/{storeId}", 999_999L, 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MARKET_NOT_FOUND"));
    }

    private Long findMarketId(String marketName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM markets WHERE name = ?",
                Long.class,
                marketName
        );
    }

    private Long findStoreId(String storeName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM stores WHERE name = ?",
                Long.class,
                storeName
        );
    }
}
