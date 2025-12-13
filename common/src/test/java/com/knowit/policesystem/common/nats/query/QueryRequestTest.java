package com.knowit.policesystem.common.nats.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for QueryRequest base class.
 */
class QueryRequestTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testQueryId_CanBeSetAndRetrieved() {
        // Given
        ExistsQueryRequest request = new ExistsQueryRequest();
        String queryId = "test-query-id-123";

        // When
        request.setQueryId(queryId);

        // Then
        assertThat(request.getQueryId()).isEqualTo(queryId);
    }

    @Test
    void testDomain_CanBeSetAndRetrieved() {
        // Given
        ExistsQueryRequest request = new ExistsQueryRequest();
        String domain = "officer";

        // When
        request.setDomain(domain);

        // Then
        assertThat(request.getDomain()).isEqualTo(domain);
    }

    @Test
    void testOperation_CanBeSetAndRetrieved() {
        // Given
        ExistsQueryRequest request = new ExistsQueryRequest();
        String operation = "exists";

        // When
        request.setOperation(operation);

        // Then
        assertThat(request.getOperation()).isEqualTo(operation);
    }

    @Test
    void testConstructor_WithAllParameters_SetsAllFields() {
        // Given
        String queryId = "query-123";
        String domain = "call";
        String resourceId = "CALL-456";

        // When
        ExistsQueryRequest request = new ExistsQueryRequest(queryId, domain, resourceId);

        // Then
        assertThat(request.getQueryId()).isEqualTo(queryId);
        assertThat(request.getDomain()).isEqualTo(domain);
        assertThat(request.getOperation()).isEqualTo("exists");
        assertThat(request.getResourceId()).isEqualTo(resourceId);
    }

    @Test
    void testConstructor_WithNullQueryId_AllowsNull() {
        // Given
        String domain = "incident";
        String resourceId = "INC-789";

        // When
        ExistsQueryRequest request = new ExistsQueryRequest(null, domain, resourceId);

        // Then
        assertThat(request.getQueryId()).isNull();
        assertThat(request.getDomain()).isEqualTo(domain);
        assertThat(request.getResourceId()).isEqualTo(resourceId);
    }

    @Test
    void testSerialization_WithJackson_Works() throws Exception {
        // Given
        ExistsQueryRequest request = new ExistsQueryRequest("query-123", "officer", "BADGE-456");

        // When
        String json = objectMapper.writeValueAsString(request);
        ExistsQueryRequest deserialized = objectMapper.readValue(json, ExistsQueryRequest.class);

        // Then
        assertThat(deserialized.getQueryId()).isEqualTo("query-123");
        assertThat(deserialized.getDomain()).isEqualTo("officer");
        assertThat(deserialized.getOperation()).isEqualTo("exists");
        assertThat(deserialized.getResourceId()).isEqualTo("BADGE-456");
    }
}
