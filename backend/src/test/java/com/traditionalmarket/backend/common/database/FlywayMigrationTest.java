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
        assertEquals("1", current.getVersion().getVersion());
        assertEquals(1, countSuccessfulVersionOneMigrations());
        assertEquals(5, countDomainTables());
    }

    private int countSuccessfulVersionOneMigrations() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = TRUE",
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
                  AND table_name IN ('markets', 'users', 'stores', 'store_favorites', 'store_reviews')
                """,
                Integer.class
        );
        return count == null ? 0 : count;
    }
}
