package com.traditionalmarket.backend.common.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class FlywayMigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesInitialSchemaMigration() {
        MigrationInfo current = flyway.info().current();

        assertNotNull(current);
        assertEquals("6", current.getVersion().getVersion());
        assertEquals(6, countSuccessfulMigrations());
        assertEquals(13, countDomainTables());
    }

    private int countSuccessfulMigrations() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = TRUE
                  AND version IS NOT NULL
                """,
                Integer.class
        );
        return count == null ? 0 : count;
    }

    private int countDomainTables() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                      'markets',
                      'users',
                      'stores',
                      'store_favorites',
                      'store_reviews',
                      'products',
                      'carts',
                      'cart_items',
                      'orders',
                      'order_items',
                      'payments',
                      'payment_cancellations',
                      'toss_webhook_events'
                  )
                """,
                Integer.class
        );
        return count == null ? 0 : count;
    }
}
