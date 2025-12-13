package com.knowit.policesystem.projection.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaValidationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testAllTablesExist() {
        // Verify all 7 main tables exist
        assertTableExists("incident_projection");
        assertTableExists("call_projection");
        assertTableExists("dispatch_projection");
        assertTableExists("activity_projection");
        assertTableExists("assignment_projection");
        assertTableExists("involved_party_projection");
        assertTableExists("resource_assignment_projection");

        // Verify all 5 status history tables exist (involved_party and resource_assignment don't have status history)
        assertTableExists("incident_status_history");
        assertTableExists("call_status_history");
        assertTableExists("dispatch_status_history");
        assertTableExists("activity_status_history");
        assertTableExists("assignment_status_history");
    }

    @Test
    void testForeignKeysExist() {
        // Verify call_projection.incident_id FK exists
        assertForeignKeyExists("call_projection", "incident_id", "incident_projection", "incident_id");

        // Verify dispatch_projection.call_id FK exists
        assertForeignKeyExists("dispatch_projection", "call_id", "call_projection", "call_id");

        // Verify activity_projection.incident_id FK exists
        assertForeignKeyExists("activity_projection", "incident_id", "incident_projection", "incident_id");

        // Verify assignment_projection FKs exist
        assertForeignKeyExists("assignment_projection", "incident_id", "incident_projection", "incident_id");
        assertForeignKeyExists("assignment_projection", "call_id", "call_projection", "call_id");
        assertForeignKeyExists("assignment_projection", "dispatch_id", "dispatch_projection", "dispatch_id");

        // Verify involved_party_projection FKs exist
        assertForeignKeyExists("involved_party_projection", "incident_id", "incident_projection", "incident_id");
        assertForeignKeyExists("involved_party_projection", "call_id", "call_projection", "call_id");
        assertForeignKeyExists("involved_party_projection", "activity_id", "activity_projection", "activity_id");

        // Verify resource_assignment_projection.assignment_id FK exists
        assertForeignKeyExists("resource_assignment_projection", "assignment_id", "assignment_projection", "assignment_id");

        // Verify status history FKs exist
        assertForeignKeyExists("incident_status_history", "incident_id", "incident_projection", "incident_id");
        assertForeignKeyExists("call_status_history", "call_id", "call_projection", "call_id");
        assertForeignKeyExists("dispatch_status_history", "dispatch_id", "dispatch_projection", "dispatch_id");
        assertForeignKeyExists("activity_status_history", "activity_id", "activity_projection", "activity_id");
        assertForeignKeyExists("assignment_status_history", "assignment_id", "assignment_projection", "assignment_id");
    }

    @Test
    void testIndexesExist() {
        // Verify composite indexes exist for common queries
        assertIndexExists("idx_call_incident");
        assertIndexExists("idx_dispatch_call");
        assertIndexExists("idx_activity_incident");
        assertIndexExists("idx_assignment_incident");
        assertIndexExists("idx_assignment_call");
        assertIndexExists("idx_assignment_dispatch");
        assertIndexExists("idx_involved_party_incident");
        assertIndexExists("idx_involved_party_call");
        assertIndexExists("idx_involved_party_activity");
        assertIndexExists("idx_resource_assignment_assignment");

        // Verify status/type filtering indexes
        assertIndexExists("idx_incident_status_priority");
        assertIndexExists("idx_call_status_priority");
        assertIndexExists("idx_dispatch_status_type");
        assertIndexExists("idx_activity_status_type");
        assertIndexExists("idx_assignment_status_type");

        // Verify status history indexes
        assertIndexExists("idx_incident_status_history_incident_changed_at");
        assertIndexExists("idx_call_status_history_call_changed_at");
        assertIndexExists("idx_dispatch_status_history_dispatch_changed_at");
        assertIndexExists("idx_activity_status_history_activity_changed_at");
        assertIndexExists("idx_assignment_status_history_assignment_changed_at");
    }

    private void assertTableExists(String tableName) {
        String sql = """
                SELECT EXISTS (
                    SELECT FROM information_schema.tables
                    WHERE table_schema = 'public'
                    AND table_name = ?
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, tableName);
        assertThat(exists).as("Table %s should exist", tableName).isTrue();
    }

    private void assertForeignKeyExists(String tableName, String columnName, String referencedTable, String referencedColumn) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                        ON tc.constraint_name = kcu.constraint_name
                    JOIN information_schema.constraint_column_usage ccu
                        ON ccu.constraint_name = tc.constraint_name
                    WHERE tc.constraint_type = 'FOREIGN KEY'
                    AND tc.table_name = ?
                    AND kcu.column_name = ?
                    AND ccu.table_name = ?
                    AND ccu.column_name = ?
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, tableName, columnName, referencedTable, referencedColumn);
        assertThat(exists).as("Foreign key from %s.%s to %s.%s should exist", tableName, columnName, referencedTable, referencedColumn).isTrue();
    }

    private void assertIndexExists(String indexName) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM pg_indexes
                    WHERE schemaname = 'public'
                    AND indexname = ?
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, indexName);
        assertThat(exists).as("Index %s should exist", indexName).isTrue();
    }
}
