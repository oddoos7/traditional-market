package com.traditionalmarket.backend.market;

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
class MarketDatabaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsMarketTableAndPersistsData() {
        int insertedRows = jdbcTemplate.update(
                """
                INSERT INTO markets (name, address, status, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                "테스트 전통시장",
                "서울특별시 테스트구 시장로 1",
                "OPEN"
        );

        Integer marketCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM markets WHERE name = ?",
                Integer.class,
                "테스트 전통시장"
        );

        assertEquals(1, insertedRows);
        assertEquals(1, marketCount);
    }
}
