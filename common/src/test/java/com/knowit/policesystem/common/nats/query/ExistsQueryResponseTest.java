package com.knowit.policesystem.common.nats.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ExistsQueryResponse.
 */
class ExistsQueryResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testExists_CanBeSetAndRetrieved() {
        // Given
        ExistsQueryResponse response = new ExistsQueryResponse();
        
        // When
        response.setExists(true);

        // Then
        assertThat(response.isExists()).isTrue();
        
        // When
        response.setExists(false);

        // Then
        assertThat(response.isExists()).isFalse();
    }

    @Test
    void testConstructor_WithExists_SetsSuccessTrue() {
        // Given
        String queryId = "query-123";
        boolean exists = true;

        // When
        ExistsQueryResponse response = new ExistsQueryResponse(queryId, exists);

        // Then
        assertThat(response.getQueryId()).isEqualTo(queryId);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isExists()).isTrue();
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    void testConstructor_WithErrorMessage_SetsSuccessFalse() {
        // Given
        String queryId = "query-123";
        String errorMessage = "Database error";

        // When
        ExistsQueryResponse response = new ExistsQueryResponse(queryId, errorMessage);

        // Then
        assertThat(response.getQueryId()).isEqualTo(queryId);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.isExists()).isFalse();
        assertThat(response.getErrorMessage()).isEqualTo(errorMessage);
    }

    @Test
    void testSerialization_WithJackson_Works() throws Exception {
        // Given
        ExistsQueryResponse response = new ExistsQueryResponse("query-123", false);

        // When
        String json = objectMapper.writeValueAsString(response);
        ExistsQueryResponse deserialized = objectMapper.readValue(json, ExistsQueryResponse.class);

        // Then
        assertThat(deserialized.getQueryId()).isEqualTo("query-123");
        assertThat(deserialized.isSuccess()).isTrue();
        assertThat(deserialized.isExists()).isFalse();
    }

    @Test
    void testDefaultConstructor_CreatesEmptyResponse() {
        // When
        ExistsQueryResponse response = new ExistsQueryResponse();

        // Then
        assertThat(response.getQueryId()).isNull();
        assertThat(response.isSuccess()).isFalse(); // Default boolean value
        assertThat(response.isExists()).isFalse();
    }
}
