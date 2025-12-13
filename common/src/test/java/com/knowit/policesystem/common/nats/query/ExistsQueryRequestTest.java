package com.knowit.policesystem.common.nats.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ExistsQueryRequest.
 */
class ExistsQueryRequestTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testResourceId_CanBeSetAndRetrieved() {
        // Given
        ExistsQueryRequest request = new ExistsQueryRequest();
        String resourceId = "RESOURCE-123";

        // When
        request.setResourceId(resourceId);

        // Then
        assertThat(request.getResourceId()).isEqualTo(resourceId);
    }

    @Test
    void testConstructor_SetsOperationToExists() {
        // Given
        String queryId = "query-123";
        String domain = "officer";
        String resourceId = "BADGE-456";

        // When
        ExistsQueryRequest request = new ExistsQueryRequest(queryId, domain, resourceId);

        // Then
        assertThat(request.getOperation()).isEqualTo("exists");
    }

    @Test
    void testSerialization_WithJackson_Works() throws Exception {
        // Given
        ExistsQueryRequest request = new ExistsQueryRequest("query-123", "call", "CALL-789");

        // When
        String json = objectMapper.writeValueAsString(request);
        ExistsQueryRequest deserialized = objectMapper.readValue(json, ExistsQueryRequest.class);

        // Then
        assertThat(deserialized.getQueryId()).isEqualTo("query-123");
        assertThat(deserialized.getDomain()).isEqualTo("call");
        assertThat(deserialized.getResourceId()).isEqualTo("CALL-789");
        assertThat(deserialized.getOperation()).isEqualTo("exists");
    }

    @Test
    void testDefaultConstructor_CreatesEmptyRequest() {
        // When
        ExistsQueryRequest request = new ExistsQueryRequest();

        // Then
        assertThat(request.getQueryId()).isNull();
        assertThat(request.getDomain()).isNull();
        assertThat(request.getResourceId()).isNull();
        assertThat(request.getOperation()).isNull();
    }
}
