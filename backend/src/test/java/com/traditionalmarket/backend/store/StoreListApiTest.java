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
class StoreListApiTest {

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
    void returnsPagedStoresBelongingToMarket() throws Exception {
        Long marketId = findMarketId("우리동네 중앙시장");

        mockMvc.perform(get("/api/markets/{marketId}/stores", marketId)
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].marketId").value(marketId))
                .andExpect(jsonPath("$.content[1].marketId").value(marketId))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void filtersStoresByKeywordCategoryAndStatus() throws Exception {
        Long marketId = findMarketId("우리동네 중앙시장");

        mockMvc.perform(get("/api/markets/{marketId}/stores", marketId)
                        .param("keyword", "수산")
                        .param("category", "SEAFOOD")
                        .param("status", "TEMPORARILY_CLOSED")
                        .param("sort", "NAME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("정다운수산"))
                .andExpect(jsonPath("$.content[0].category").value("SEAFOOD"))
                .andExpect(jsonPath("$.content[0].status").value("TEMPORARILY_CLOSED"));
    }

    @Test
    void returnsNotFoundWhenMarketDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/markets/{marketId}/stores", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MARKET_NOT_FOUND"));
    }

    @Test
    void rejectsPageSizeOverLimit() throws Exception {
        Long marketId = findMarketId("우리동네 중앙시장");

        mockMvc.perform(get("/api/markets/{marketId}/stores", marketId)
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsUnknownCategory() throws Exception {
        Long marketId = findMarketId("우리동네 중앙시장");

        mockMvc.perform(get("/api/markets/{marketId}/stores", marketId)
                        .param("category", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private Long findMarketId(String marketName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM markets WHERE name = ?",
                Long.class,
                marketName
        );
    }
}
