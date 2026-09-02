package com.traditionalmarket.backend.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class MarketCommandApiTest {

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
    void createsMarket() throws Exception {
        String request = """
                {
                  "name": "새봄 전통시장",
                  "description": "제철 농산물과 먹거리가 풍성한 시장입니다.",
                  "address": "서울특별시 새봄구 시장길 10",
                  "detailAddress": "상가동 1층",
                  "phone": "02-1234-5678",
                  "openingHours": "09:00 ~ 20:00",
                  "closedDays": "매월 첫째 주 월요일",
                  "latitude": 37.5123456,
                  "longitude": 127.0123456,
                  "imageUrl": "https://example.com/markets/new-spring.jpg",
                  "status": "OPEN"
                }
                """;

        mockMvc.perform(post("/api/markets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("새봄 전통시장"))
                .andExpect(jsonPath("$.address").value("서울특별시 새봄구 시장길 10"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        Integer savedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM markets WHERE name = ?",
                Integer.class,
                "새봄 전통시장"
        );

        assertEquals(1, savedCount);
    }

    @Test
    void updatesMarket() throws Exception {
        Long marketId = findMarketId("우리동네 중앙시장");
        String request = """
                {
                  "name": "우리동네 중앙시장 새단장",
                  "description": "고객 편의시설을 새롭게 정비한 전통시장입니다.",
                  "address": "서울특별시 행복구 중앙시장길 12",
                  "detailAddress": "고객지원센터 2층",
                  "phone": "02-1111-2222",
                  "openingHours": "08:30 ~ 21:00",
                  "closedDays": "연중무휴",
                  "latitude": 37.5665000,
                  "longitude": 126.9780000,
                  "imageUrl": null,
                  "status": "TEMPORARILY_CLOSED"
                }
                """;

        mockMvc.perform(put("/api/markets/{marketId}", marketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(marketId))
                .andExpect(jsonPath("$.name").value("우리동네 중앙시장 새단장"))
                .andExpect(jsonPath("$.detailAddress").value("고객지원센터 2층"))
                .andExpect(jsonPath("$.phone").value("02-1111-2222"))
                .andExpect(jsonPath("$.openingHours").value("08:30 ~ 21:00"))
                .andExpect(jsonPath("$.status").value("TEMPORARILY_CLOSED"))
                .andExpect(jsonPath("$.updatedAt").exists());

        String savedName = jdbcTemplate.queryForObject(
                "SELECT name FROM markets WHERE id = ?",
                String.class,
                marketId
        );

        assertEquals("우리동네 중앙시장 새단장", savedName);
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        String request = """
                {
                  "name": " ",
                  "address": " ",
                  "latitude": 91,
                  "longitude": 181,
                  "status": null
                }
                """;

        mockMvc.perform(post("/api/markets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void rejectsUnknownMarketStatus() throws Exception {
        String request = """
                {
                  "name": "테스트 시장",
                  "address": "서울특별시 테스트구 시장로 1",
                  "status": "UNKNOWN"
                }
                """;

        mockMvc.perform(post("/api/markets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void returnsNotFoundWhenUpdatingMissingMarket() throws Exception {
        String request = """
                {
                  "name": "없는 시장",
                  "address": "서울특별시 테스트구 시장로 2",
                  "status": "OPEN"
                }
                """;

        mockMvc.perform(put("/api/markets/{marketId}", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
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
}
