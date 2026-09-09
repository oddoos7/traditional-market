package com.traditionalmarket.backend.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.traditionalmarket.backend.review.application.StoreReviewService;
import com.traditionalmarket.backend.review.dto.request.ReviewSaveRequest;
import com.traditionalmarket.backend.review.exception.DuplicateReviewException;
import com.traditionalmarket.backend.review.repository.StoreReviewRepository;

@SpringBootTest
@ActiveProfiles("local")
class StoreReviewConcurrencyTest {

    private static final String ADMIN_EMAIL = "admin@traditional-market.local";

    @Autowired
    private StoreReviewService storeReviewService;

    @Autowired
    private StoreReviewRepository storeReviewRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentReviewRequestsCreateOnlyOneRow() throws Exception {
        Long userId = findUserId(ADMIN_EMAIL);
        Long storeId = findStoreId("행복청과");
        deleteReview(userId, storeId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<CreationResult> firstRequest = executor.submit(
                    () -> createReviewWhenStarted(storeId, ready, start)
            );
            Future<CreationResult> secondRequest = executor.submit(
                    () -> createReviewWhenStarted(storeId, ready, start)
            );

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<CreationResult> results = List.of(
                    firstRequest.get(10, TimeUnit.SECONDS),
                    secondRequest.get(10, TimeUnit.SECONDS)
            );

            assertEquals(1, results.stream().filter(CreationResult.CREATED::equals).count());
            assertEquals(1, results.stream().filter(CreationResult.DUPLICATE::equals).count());
            assertEquals(1, storeReviewRepository.countByUserIdAndStoreId(userId, storeId));
        } finally {
            start.countDown();
            executor.shutdownNow();
            deleteReview(userId, storeId);
        }
    }

    private CreationResult createReviewWhenStarted(
            Long storeId,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        try {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 요청 시작 신호를 기다리는 시간이 초과됐습니다.");
            }

            storeReviewService.createReview(
                    ADMIN_EMAIL,
                    storeId,
                    new ReviewSaveRequest(5, "동시 요청으로 등록하는 리뷰입니다.")
            );
            return CreationResult.CREATED;
        } catch (DuplicateReviewException exception) {
            return CreationResult.DUPLICATE;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시 요청 테스트가 중단됐습니다.", exception);
        }
    }

    private Long findUserId(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
        );
    }

    private Long findStoreId(String storeName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM stores WHERE name = ?",
                Long.class,
                storeName
        );
    }

    private void deleteReview(Long userId, Long storeId) {
        jdbcTemplate.update(
                "DELETE FROM store_reviews WHERE user_id = ? AND store_id = ?",
                userId,
                storeId
        );
    }

    private enum CreationResult {
        CREATED,
        DUPLICATE
    }
}
