package com.traditionalmarket.backend.market;

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
class MarketDetailApiTest {

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
    void returnsMarketDetail() throws Exception {
        Long marketId = findMarketId("우리동네 중앙시장");

        mockMvc.perform(get("/api/markets/{marketId}", marketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(marketId))
                .andExpect(jsonPath("$.name").value("우리동네 중앙시장"))
                .andExpect(jsonPath("$.address").value("서울특별시 행복구 중앙시장길 12"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void returnsNotFoundWhenMarketDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/markets/{marketId}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MARKET_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("시장을 찾을 수 없습니다. marketId=999999"));
    }

    @Test
    void rejectsNonPositiveMarketId() throws Exception {
        mockMvc.perform(get("/api/markets/{marketId}", 0))
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
