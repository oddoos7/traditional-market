package com.traditionalmarket.backend.favorite;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import com.traditionalmarket.backend.favorite.application.StoreFavoriteService;
import com.traditionalmarket.backend.favorite.repository.StoreFavoriteRepository;

@SpringBootTest
@ActiveProfiles("local")
class StoreFavoriteConcurrencyTest {

    private static final String ADMIN_EMAIL = "admin@traditional-market.local";

    @Autowired
    private StoreFavoriteService storeFavoriteService;

    @Autowired
    private StoreFavoriteRepository storeFavoriteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentFavoriteRequestsCreateOnlyOneRow() throws Exception {
        Long userId = findUserId(ADMIN_EMAIL);
        Long storeId = findStoreId("행복청과");
        deleteFavorite(userId, storeId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?> firstRequest = executor.submit(() -> addFavoriteWhenStarted(storeId, ready, start));
            Future<?> secondRequest = executor.submit(() -> addFavoriteWhenStarted(storeId, ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            assertDoesNotThrow(() -> {
                firstRequest.get(10, TimeUnit.SECONDS);
                secondRequest.get(10, TimeUnit.SECONDS);
            });

            assertEquals(
                    1,
                    storeFavoriteRepository.countByUserIdAndStoreId(userId, storeId)
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
            deleteFavorite(userId, storeId);
        }
    }

    private void addFavoriteWhenStarted(
            Long storeId,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        try {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 요청 시작 신호를 기다리는 시간이 초과됐습니다.");
            }
            storeFavoriteService.addFavorite(ADMIN_EMAIL, storeId);
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

    private void deleteFavorite(Long userId, Long storeId) {
        jdbcTemplate.update(
                "DELETE FROM store_favorites WHERE user_id = ? AND store_id = ?",
                userId,
                storeId
        );
    }
}
