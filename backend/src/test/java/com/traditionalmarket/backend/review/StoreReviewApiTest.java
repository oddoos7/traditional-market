package com.traditionalmarket.backend.review;

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
class StoreReviewApiTest {

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
    void createsListsUpdatesAndDeletesOwnReview() throws Exception {
        Long storeId = findStoreId("정다운수산");

        mockMvc.perform(post("/api/stores/{storeId}/reviews", storeId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(5, "신선한 해산물이 많고 사장님도 친절해요.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeId").value(storeId))
                .andExpect(jsonPath("$.reviewerName").isString())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.mine").value(true));

        Long reviewId = findReviewId(storeId, ADMIN_EMAIL);

        mockMvc.perform(get("/api/stores/{storeId}/reviews", storeId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(reviewId))
                .andExpect(jsonPath("$.content[0].mine").value(true))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.averageRating").value(5.0));

        mockMvc.perform(get("/api/stores/{storeId}/reviews/me", storeId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId));

        mockMvc.perform(put("/api/stores/{storeId}/reviews/{reviewId}", storeId, reviewId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(4, "Satisfied on my second visit.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.content").value("Satisfied on my second visit."));

        mockMvc.perform(delete(
                        "/api/stores/{storeId}/reviews/{reviewId}",
                        storeId,
                        reviewId
                )
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/stores/{storeId}/reviews", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsSecondReviewForSameUserAndStore() throws Exception {
        Long storeId = findStoreId("정다운수산");

        createReview(storeId, ADMIN_EMAIL, "ADMIN");

        mockMvc.perform(post("/api/stores/{storeId}/reviews", storeId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(3, "두 번째 리뷰는 등록되면 안 됩니다.")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVIEW_ALREADY_EXISTS"));
    }

    @Test
    void allowsAnonymousReviewListButRequiresLoginForWrite() throws Exception {
        Long storeId = findStoreId("정다운수산");

        mockMvc.perform(get("/api/stores/{storeId}/reviews", storeId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/stores/{storeId}/reviews", storeId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(5, "로그인하지 않은 리뷰입니다.")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsReviewUpdateFromDifferentUser() throws Exception {
        Long storeId = findStoreId("정다운수산");
        createReview(storeId, ADMIN_EMAIL, "ADMIN");
        Long reviewId = findReviewId(storeId, ADMIN_EMAIL);
        String memberEmail = "reviewer@example.com";

        jdbcTemplate.update(
                """
                INSERT INTO users (email, password_hash, name, role, created_at, updated_at)
                VALUES (?, ?, ?, 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                memberEmail,
                "test-password-hash",
                "다른 회원"
        );

        mockMvc.perform(put("/api/stores/{storeId}/reviews/{reviewId}", storeId, reviewId)
                        .with(user(memberEmail).roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(1, "다른 사람의 리뷰를 수정하려고 합니다.")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void rejectsInvalidRatingAndContent() throws Exception {
        Long storeId = findStoreId("정다운수산");

        mockMvc.perform(post("/api/stores/{storeId}/reviews", storeId)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(6, " ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsNonPositiveReviewIds() throws Exception {
        Long storeId = findStoreId("정다운수산");

        mockMvc.perform(put("/api/stores/{storeId}/reviews/{reviewId}", storeId, 0)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(4, "수정할 리뷰 ID는 양수여야 합니다.")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(delete(
                        "/api/stores/{storeId}/reviews/{reviewId}",
                        storeId,
                        -1
                )
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsNonPositiveStoreIds() throws Exception {
        mockMvc.perform(get("/api/stores/{storeId}/reviews", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/api/stores/{storeId}/reviews/me", -1)
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/stores/{storeId}/reviews", 0)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(5, "점포 ID는 양수여야 합니다.")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(put("/api/stores/{storeId}/reviews/{reviewId}", -1, 1)
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(4, "점포 ID는 양수여야 합니다.")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(delete(
                        "/api/stores/{storeId}/reviews/{reviewId}",
                        0,
                        1
                )
                        .with(user(ADMIN_EMAIL).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void returnsNotFoundForUnknownStore() throws Exception {
        mockMvc.perform(get("/api/stores/{storeId}/reviews", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STORE_NOT_FOUND"));
    }

    private void createReview(Long storeId, String email, String role) throws Exception {
        mockMvc.perform(post("/api/stores/{storeId}/reviews", storeId)
                        .with(user(email).roles(role))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(5, "신선하고 친절해서 다시 방문하고 싶어요.")))
                .andExpect(status().isCreated());
    }

    private Long findStoreId(String storeName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM stores WHERE name = ?",
                Long.class,
                storeName
        );
    }

    private Long findReviewId(Long storeId, String email) {
        return jdbcTemplate.queryForObject(
                """
                SELECT sr.id
                FROM store_reviews sr
                JOIN users u ON u.id = sr.user_id
                WHERE sr.store_id = ? AND u.email = ?
                """,
                Long.class,
                storeId,
                email
        );
    }

    private String reviewRequest(int rating, String content) {
        return """
                {
                  "rating": %d,
                  "content": "%s"
                }
                """.formatted(rating, content);
    }
}
