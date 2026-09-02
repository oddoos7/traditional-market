package com.traditionalmarket.backend.store;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class StoreDatabaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsStoreTableAndPersistsMarketRelationship() {
        Long marketId = findMarketId("우리동네 중앙시장");

        int insertedRows = jdbcTemplate.update(
                """
                INSERT INTO stores (
                    market_id, name, category, status, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                marketId,
                "테스트 점포",
                "ETC",
                "OPEN"
        );

        Long savedMarketId = jdbcTemplate.queryForObject(
                "SELECT market_id FROM stores WHERE name = ?",
                Long.class,
                "테스트 점포"
        );

        assertEquals(1, insertedRows);
        assertEquals(marketId, savedMarketId);
    }

    private Long findMarketId(String marketName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM markets WHERE name = ?",
                Long.class,
                marketName
        );
    }
}
