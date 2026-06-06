package com.qwenpaw.clawmgt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SchemaMigrationTests {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesCoreTables() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public' and upper(table_name) in " +
                        "('CHANNELS','NODES','TASKS','TASK_ITEMS','TASK_ITEM_DETAILS','TASK_EVENTS','SESSIONS','MESSAGES','NODE_REPORTS','NODE_SKILL_METADATA')",
                Integer.class);
        assertThat(count).isEqualTo(10);
    }

    @Test
    void taskTablesDoNotPersistCategory() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns " +
                        "where table_schema = 'public' " +
                        "and upper(table_name) in ('TASKS', 'TASK_ITEMS') " +
                        "and upper(column_name) = 'CATEGORY'",
                Integer.class);
        assertThat(count).isZero();
    }
}
