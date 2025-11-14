package com.knowit.policesystem.edge.queries;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Query base class functionality including ID generation,
 * timestamp setting, and query type.
 */
class QueryTest {

    @Test
    void testQuery_WithDefaultConstructor_GeneratesIdAndTimestamp() {
        // Given & When
        TestQuery query = new TestQuery();

        // Then
        assertThat(query.getQueryId()).isNotNull();
        assertThat(query.getQueryId()).isInstanceOf(UUID.class);
        assertThat(query.getTimestamp()).isNotNull();
        assertThat(query.getTimestamp()).isInstanceOf(Instant.class);
        assertThat(query.getQueryType()).isEqualTo("TestQuery");
    }

    @Test
    void testQuery_WithParameters_SetsFields() {
        // Given
        String searchTerm = "test search";
        int limit = 10;

        // When
        TestQuery query = new TestQuery(searchTerm, limit);

        // Then
        assertThat(query.getQueryId()).isNotNull();
        assertThat(query.getTimestamp()).isNotNull();
        assertThat(query.getSearchTerm()).isEqualTo(searchTerm);
        assertThat(query.getLimit()).isEqualTo(limit);
        assertThat(query.getQueryType()).isEqualTo("TestQuery");
    }

    @Test
    void testQuery_WithFullConstructor_SetsAllFields() {
        // Given
        UUID queryId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        String searchTerm = "full constructor test";
        int limit = 20;

        // When
        TestQuery query = new TestQuery(queryId, timestamp, searchTerm, limit);

        // Then
        assertThat(query.getQueryId()).isEqualTo(queryId);
        assertThat(query.getTimestamp()).isEqualTo(timestamp);
        assertThat(query.getSearchTerm()).isEqualTo(searchTerm);
        assertThat(query.getLimit()).isEqualTo(limit);
        assertThat(query.getQueryType()).isEqualTo("TestQuery");
    }

    @Test
    void testQuery_TimestampIsSetOnCreation() {
        // Given
        Instant beforeCreation = Instant.now();

        // When
        TestQuery query = new TestQuery();
        Instant afterCreation = Instant.now();

        // Then
        assertThat(query.getTimestamp()).isAfterOrEqualTo(beforeCreation);
        assertThat(query.getTimestamp()).isBeforeOrEqualTo(afterCreation);
    }

    @Test
    void testQuery_EachQueryHasUniqueId() {
        // When
        TestQuery query1 = new TestQuery();
        TestQuery query2 = new TestQuery();

        // Then
        assertThat(query1.getQueryId()).isNotEqualTo(query2.getQueryId());
    }
}

