package com.traditionalmarket.backend.favorite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class StoreFavoriteApiTest {

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
    void addsListsAndRemovesFavorite() throws Exception {
        Long storeId = findStoreId("행복청과");

        mockMvc.perform(get("/api/favorites/stores/{storeId}/status", storeId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(false));

        mockMvc.perform(post("/api/favorites/stores/{storeId}", storeId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.favorite").value(true));

        mockMvc.perform(get("/api/favorites/stores")
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storeId").value(storeId))
                .andExpect(jsonPath("$[0].name").value("행복청과"))
                .andExpect(jsonPath("$[0].marketName").value("우리동네 중앙시장"));

        mockMvc.perform(delete("/api/favorites/stores/{storeId}", storeId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/favorites/stores/{storeId}/status", storeId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(false));
    }

    @Test
    void addingSameStoreTwiceKeepsSingleFavorite() throws Exception {
        Long storeId = findStoreId("행복청과");

        mockMvc.perform(post("/api/favorites/stores/{storeId}", storeId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/favorites/stores/{storeId}", storeId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isCreated());

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM store_favorites sf
                JOIN users u ON u.id = sf.user_id
                WHERE u.email = ? AND sf.store_id = ?
                """,
                Integer.class,
                ADMIN_EMAIL,
                storeId
        );
        assertEquals(1, count);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/favorites/stores"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void returnsNotFoundForUnknownStore() throws Exception {
        mockMvc.perform(post("/api/favorites/stores/{storeId}", 999_999L)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STORE_NOT_FOUND"));
    }

    @Test
    void rejectsNonPositiveStoreIds() throws Exception {
        mockMvc.perform(get("/api/favorites/stores/{storeId}/status", 0)
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/favorites/stores/{storeId}", -1)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(delete("/api/favorites/stores/{storeId}", 0)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
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
}
