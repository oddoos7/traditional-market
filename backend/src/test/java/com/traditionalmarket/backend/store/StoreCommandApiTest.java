package com.traditionalmarket.backend.store;

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
class StoreCommandApiTest {

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
    void createsStoreAsAdmin() throws Exception {
        Long marketId = findMarketId("우리동네 중앙시장");

        mockMvc.perform(post("/api/markets/{marketId}/stores", marketId)
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("새봄채소", "AGRICULTURAL", "가동 20호", "OPEN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.marketId").value(marketId))
                .andExpect(jsonPath("$.marketName").value("우리동네 중앙시장"))
                .andExpect(jsonPath("$.name").value("새봄채소"))
                .andExpect(jsonPath("$.category").value("AGRICULTURAL"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stores WHERE market_id = ? AND name = ?",
                Integer.class,
                marketId,
                "새봄채소"
        );
        assertEquals(1, count);
    }

    @Test
    void updatesStoreAsAdmin() throws Exception {
        Long marketId = findMarketId("우리동네 중앙시장");
        Long storeId = findStoreId("행복청과");

        mockMvc.perform(put("/api/markets/{marketId}/stores/{storeId}", marketId, storeId)
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("행복청과 새단장", "FOOD", "가동 21호", "TEMPORARILY_CLOSED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(storeId))
                .andExpect(jsonPath("$.name").value("행복청과 새단장"))
                .andExpect(jsonPath("$.category").value("FOOD"))
                .andExpect(jsonPath("$.location").value("가동 21호"))
                .andExpect(jsonPath("$.status").value("TEMPORARILY_CLOSED"));
    }

    @Test
    void rejectsStoreCreateFromRegularUser() throws Exception {
        Long marketId = findMarketId("우리동네 중앙시장");

        mockMvc.perform(post("/api/markets/{marketId}/stores", marketId)
                        .with(user("member@example.com").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("일반회원점포", "ETC", null, "OPEN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void requiresAuthenticationWhenCreatingStore() throws Exception {
        Long marketId = findMarketId("우리동네 중앙시장");

        mockMvc.perform(post("/api/markets/{marketId}/stores", marketId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("비로그인점포", "ETC", null, "OPEN")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void returnsNotFoundWhenUpdatingStoreFromAnotherMarket() throws Exception {
        Long centralMarketId = findMarketId("우리동네 중앙시장");
        Long alleyStoreId = findStoreId("푸른떡방");

        mockMvc.perform(put(
                        "/api/markets/{marketId}/stores/{storeId}",
                        centralMarketId,
                        alleyStoreId
                )
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("잘못된수정", "ETC", null, "OPEN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STORE_NOT_FOUND"));
    }

    @Test
    void rejectsInvalidStoreRequest() throws Exception {
        Long marketId = findMarketId("우리동네 중앙시장");
        String invalidRequest = """
                {
                  "name": " ",
                  "category": null,
                  "status": null
                }
                """;

        mockMvc.perform(post("/api/markets/{marketId}/stores", marketId)
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
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

    private Long findStoreId(String storeName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM stores WHERE name = ?",
                Long.class,
                storeName
        );
    }

    private String validRequest(String name, String category, String location, String status) {
        String locationJson = location == null ? "null" : "\"" + location + "\"";
        return """
                {
                  "name": "%s",
                  "description": "정성을 담아 운영하는 전통시장 점포입니다.",
                  "category": "%s",
                  "location": %s,
                  "phone": "02-5555-7777",
                  "openingHours": "09:00 ~ 19:00",
                  "closedDays": "매주 월요일",
                  "imageUrl": null,
                  "status": "%s"
                }
                """.formatted(name, category, locationJson, status);
    }
}
