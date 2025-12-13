package com.knowit.policesystem.common.nats.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for QueryResponse base class.
 */
class QueryResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testQueryId_CanBeSetAndRetrieved() {
        // Given
        ExistsQueryResponse response = new ExistsQueryResponse();
        String queryId = "test-query-id-123";

        // When
        response.setQueryId(queryId);

        // Then
        assertThat(response.getQueryId()).isEqualTo(queryId);
    }

    @Test
    void testSuccess_CanBeSetAndRetrieved() {
        // Given
        ExistsQueryResponse response = new ExistsQueryResponse();
        
        // When
        response.setSuccess(true);

        // Then
        assertThat(response.isSuccess()).isTrue();
        
        // When
        response.setSuccess(false);

        // Then
        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    void testErrorMessage_CanBeSetAndRetrieved() {
        // Given
        ExistsQueryResponse response = new ExistsQueryResponse();
        String errorMessage = "Resource not found";

        // When
        response.setErrorMessage(errorMessage);

        // Then
        assertThat(response.getErrorMessage()).isEqualTo(errorMessage);
    }

    @Test
    void testError_StaticMethod_CreatesErrorResponse() {
        // Given
        String queryId = "query-123";
        String errorMessage = "Something went wrong";

        // When
        QueryResponse response = QueryResponse.error(queryId, errorMessage);

        // Then
        assertThat(response.getQueryId()).isEqualTo(queryId);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).isEqualTo(errorMessage);
    }

    @Test
    void testSerialization_WithJackson_Works() throws Exception {
        // Given
        ExistsQueryResponse response = new ExistsQueryResponse("query-123", true);

        // When
        String json = objectMapper.writeValueAsString(response);
        ExistsQueryResponse deserialized = objectMapper.readValue(json, ExistsQueryResponse.class);

        // Then
        assertThat(deserialized.getQueryId()).isEqualTo("query-123");
        assertThat(deserialized.isSuccess()).isTrue();
        assertThat(deserialized.isExists()).isTrue();
    }
}
