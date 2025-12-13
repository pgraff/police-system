package com.knowit.policesystem.projection.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.nats.query.ExistsQueryRequest;
import com.knowit.policesystem.common.nats.query.ExistsQueryResponse;
import com.knowit.policesystem.common.nats.query.GetQueryRequest;
import com.knowit.policesystem.common.nats.query.GetQueryResponse;
import com.knowit.policesystem.projection.api.CallProjectionResponse;
import com.knowit.policesystem.projection.config.NatsProperties;
import com.knowit.policesystem.projection.service.CallProjectionService;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for CallNatsQueryHandler.
 * Uses NATS test container for integration-style tests.
 */
@Testcontainers
@ExtendWith(MockitoExtension.class)
class CallNatsQueryHandlerTest {

    @Container
    static GenericContainer<?> natsContainer = new GenericContainer<>(DockerImageName.parse("nats:2.10-alpine"))
            .withCommand("-js", "-m", "8222")
            .withExposedPorts(4222, 8222)
            .waitingFor(Wait.forHttp("/healthz").forPort(8222).withStartupTimeout(Duration.ofSeconds(30)));

    @Mock
    private CallProjectionService projectionService;

    private NatsProperties natsProperties;
    private ObjectMapper objectMapper;
    private CallNatsQueryHandler queryHandler;
    private String natsUrl;
    private Connection testConnection;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        natsUrl = String.format("nats://%s:%d", natsContainer.getHost(), natsContainer.getMappedPort(4222));

        natsProperties = new NatsProperties();
        natsProperties.setUrl(natsUrl);
        natsProperties.setEnabled(true);
        natsProperties.setQueryEnabled(true);
        natsProperties.setQuerySubjectPrefix("query");

        queryHandler = new CallNatsQueryHandler(natsProperties, projectionService, objectMapper);
        queryHandler.start();

        // Give handler time to subscribe
        Thread.sleep(500);

        testConnection = Nats.connect(natsUrl);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (queryHandler != null) {
            queryHandler.shutdown();
        }
        if (testConnection != null) {
            testConnection.close();
        }
    }

    @Test
    void testHandleExistsQuery_ResourceExists_ReturnsTrue() throws Exception {
        // Given
        String callId = "CALL-123";
        when(projectionService.getProjection(callId))
                .thenReturn(Optional.of(createMockCall(callId)));

        ExistsQueryRequest request = new ExistsQueryRequest("test-query-id", "call", callId);
        String requestJson = objectMapper.writeValueAsString(request);

        // When
        Message response = testConnection.request("query.call.exists", requestJson.getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(5));

        // Then
        assertThat(response).isNotNull();
        String responseJson = new String(response.getData(), StandardCharsets.UTF_8);
        ExistsQueryResponse existsResponse = objectMapper.readValue(responseJson, ExistsQueryResponse.class);
        assertThat(existsResponse.isSuccess()).isTrue();
        assertThat(existsResponse.isExists()).isTrue();
        assertThat(existsResponse.getQueryId()).isEqualTo("test-query-id");
    }

    @Test
    void testHandleExistsQuery_ResourceNotExists_ReturnsFalse() throws Exception {
        // Given
        String callId = "CALL-456";
        when(projectionService.getProjection(callId))
                .thenReturn(Optional.empty());

        ExistsQueryRequest request = new ExistsQueryRequest("test-query-id-2", "call", callId);
        String requestJson = objectMapper.writeValueAsString(request);

        // When
        Message response = testConnection.request("query.call.exists", requestJson.getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(5));

        // Then
        assertThat(response).isNotNull();
        String responseJson = new String(response.getData(), StandardCharsets.UTF_8);
        ExistsQueryResponse existsResponse = objectMapper.readValue(responseJson, ExistsQueryResponse.class);
        assertThat(existsResponse.isSuccess()).isTrue();
        assertThat(existsResponse.isExists()).isFalse();
    }

    @Test
    void testHandleGetQuery_ResourceExists_ReturnsData() throws Exception {
        // Given
        String callId = "CALL-789";
        CallProjectionResponse call = createMockCall(callId);
        when(projectionService.getProjection(callId))
                .thenReturn(Optional.of(call));

        GetQueryRequest request = new GetQueryRequest("test-get-query-id", "call", callId);
        String requestJson = objectMapper.writeValueAsString(request);

        // When
        Message response = testConnection.request("query.call.get", requestJson.getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(5));

        // Then
        assertThat(response).isNotNull();
        String responseJson = new String(response.getData(), StandardCharsets.UTF_8);
        GetQueryResponse getResponse = objectMapper.readValue(responseJson, GetQueryResponse.class);
        assertThat(getResponse.isSuccess()).isTrue();
        assertThat(getResponse.getData()).isNotNull();
    }

    @Test
    void testHandleExistsQuery_RepositoryError_HandlesGracefully() throws Exception {
        // Given
        String callId = "CALL-ERROR";
        when(projectionService.getProjection(callId))
                .thenThrow(new RuntimeException("Database error"));

        ExistsQueryRequest request = new ExistsQueryRequest("test-error-query-id", "call", callId);
        String requestJson = objectMapper.writeValueAsString(request);

        // When
        Message response = testConnection.request("query.call.exists", requestJson.getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(5));

        // Then - should return error response
        assertThat(response).isNotNull();
        String responseJson = new String(response.getData(), StandardCharsets.UTF_8);
        GetQueryResponse errorResponse = objectMapper.readValue(responseJson, GetQueryResponse.class);
        assertThat(errorResponse.isSuccess()).isFalse();
        assertThat(errorResponse.getErrorMessage()).contains("Failed to process exists query");
    }

    @Test
    void testSubscribe_SubscribesToCorrectSubject() throws Exception {
        // Given - handler is already started in setUp
        // The handler subscribes to "query.call.>"

        // When - send a request to the exists subject
        ExistsQueryRequest request = new ExistsQueryRequest("test-subject-id", "call", "CALL-TEST");
        String requestJson = objectMapper.writeValueAsString(request);

        // Then - should receive response (proves subscription works)
        Message response = testConnection.request("query.call.exists", requestJson.getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(5));
        assertThat(response).isNotNull();
    }

    private CallProjectionResponse createMockCall(String callId) {
        return new CallProjectionResponse(
                callId,
                "2024-CALL-001",
                "High",
                "Received",
                Instant.now(),
                null,
                null,
                null,
                "Test call",
                "Emergency",
                Instant.now()
        );
    }
}
