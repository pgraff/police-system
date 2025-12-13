package com.knowit.policesystem.common.nats.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GetQueryRequest.
 */
class GetQueryRequestTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testResourceId_CanBeSetAndRetrieved() {
        // Given
        GetQueryRequest request = new GetQueryRequest();
        String resourceId = "RESOURCE-123";

        // When
        request.setResourceId(resourceId);

        // Then
        assertThat(request.getResourceId()).isEqualTo(resourceId);
    }

    @Test
    void testConstructor_SetsOperationToGet() {
        // Given
        String queryId = "query-123";
        String domain = "incident";
        String resourceId = "INC-456";

        // When
        GetQueryRequest request = new GetQueryRequest(queryId, domain, resourceId);

        // Then
        assertThat(request.getOperation()).isEqualTo("get");
    }

    @Test
    void testSerialization_WithJackson_Works() throws Exception {
        // Given
        GetQueryRequest request = new GetQueryRequest("query-123", "activity", "ACT-789");

        // When
        String json = objectMapper.writeValueAsString(request);
        GetQueryRequest deserialized = objectMapper.readValue(json, GetQueryRequest.class);

        // Then
        assertThat(deserialized.getQueryId()).isEqualTo("query-123");
        assertThat(deserialized.getDomain()).isEqualTo("activity");
        assertThat(deserialized.getResourceId()).isEqualTo("ACT-789");
        assertThat(deserialized.getOperation()).isEqualTo("get");
    }

    @Test
    void testDefaultConstructor_CreatesEmptyRequest() {
        // When
        GetQueryRequest request = new GetQueryRequest();

        // Then
        assertThat(request.getQueryId()).isNull();
        assertThat(request.getDomain()).isNull();
        assertThat(request.getResourceId()).isNull();
        assertThat(request.getOperation()).isNull();
    }
}
