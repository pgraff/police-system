package com.knowit.policesystem.common.nats.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GetQueryResponse.
 */
class GetQueryResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testData_CanBeSetAndRetrieved() {
        // Given
        GetQueryResponse response = new GetQueryResponse();
        Map<String, Object> data = Map.of("id", "RESOURCE-123", "name", "Test Resource");

        // When
        response.setData(data);

        // Then
        assertThat(response.getData()).isEqualTo(data);
    }

    @Test
    void testConstructor_WithData_SetsSuccessTrue() {
        // Given
        String queryId = "query-123";
        Map<String, Object> data = Map.of("id", "RESOURCE-456");

        // When
        GetQueryResponse response = new GetQueryResponse(queryId, data);

        // Then
        assertThat(response.getQueryId()).isEqualTo(queryId);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    void testConstructor_WithErrorMessage_SetsSuccessFalse() {
        // Given
        String queryId = "query-123";
        String errorMessage = "Resource not found";

        // When
        GetQueryResponse response = new GetQueryResponse(queryId, errorMessage);

        // Then
        assertThat(response.getQueryId()).isEqualTo(queryId);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getErrorMessage()).isEqualTo(errorMessage);
    }

    @Test
    void testConstructor_WithNullData_AllowsNull() {
        // Given
        String queryId = "query-123";

        // When
        GetQueryResponse response = new GetQueryResponse(queryId, null);

        // Then
        assertThat(response.getQueryId()).isEqualTo(queryId);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
    }

    @Test
    void testSerialization_WithJackson_Works() throws Exception {
        // Given
        Map<String, Object> data = Map.of("id", "RESOURCE-789", "status", "active");
        GetQueryResponse response = new GetQueryResponse("query-123", data);

        // When
        String json = objectMapper.writeValueAsString(response);
        GetQueryResponse deserialized = objectMapper.readValue(json, GetQueryResponse.class);

        // Then
        assertThat(deserialized.getQueryId()).isEqualTo("query-123");
        assertThat(deserialized.isSuccess()).isTrue();
        assertThat(deserialized.getData()).isNotNull();
    }

    @Test
    void testDefaultConstructor_CreatesEmptyResponse() {
        // When
        GetQueryResponse response = new GetQueryResponse();

        // Then
        assertThat(response.getQueryId()).isNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
    }
}
