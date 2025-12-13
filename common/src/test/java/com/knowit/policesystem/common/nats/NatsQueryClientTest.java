package com.knowit.policesystem.common.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.nats.query.ExistsQueryRequest;
import com.knowit.policesystem.common.nats.query.ExistsQueryResponse;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for NatsQueryClient.
 * Uses NATS test container for integration-style tests.
 */
@Testcontainers
class NatsQueryClientTest {

    @Container
    static GenericContainer<?> natsContainer = new GenericContainer<>(DockerImageName.parse("nats:2.10-alpine"))
            .withCommand("-js", "-m", "8222")
            .withExposedPorts(4222, 8222)
            .waitingFor(Wait.forHttp("/healthz").forPort(8222).withStartupTimeout(Duration.ofSeconds(30)));

    private NatsQueryClient queryClient;
    private ObjectMapper objectMapper;
    private String natsUrl;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        natsUrl = String.format("nats://%s:%d", natsContainer.getHost(), natsContainer.getMappedPort(4222));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (queryClient != null) {
            queryClient.close();
        }
    }

    @Test
    void testQuery_SuccessfulRequest_ReturnsResponse() throws Exception {
        // Given - set up responder
        String subject = "query.officer.exists";
        Connection responderConnection = Nats.connect(natsUrl);
        Dispatcher dispatcher = responderConnection.createDispatcher(msg -> {
            try {
                // Deserialize request to get the queryId
                ExistsQueryRequest request = objectMapper.readValue(msg.getData(), ExistsQueryRequest.class);
                ExistsQueryResponse response = new ExistsQueryResponse(request.getQueryId(), true);
                String responseJson = objectMapper.writeValueAsString(response);
                responderConnection.publish(msg.getReplyTo(), responseJson.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                // Ignore
            }
        });
        dispatcher.subscribe(subject);

        // Create client
        queryClient = new NatsQueryClient(natsUrl, objectMapper, 5000, true);
        ExistsQueryRequest request = new ExistsQueryRequest("test-query-id", "officer", "BADGE-123");

        // When
        ExistsQueryResponse response = queryClient.query(subject, request, ExistsQueryResponse.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getQueryId()).isEqualTo("test-query-id");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isExists()).isTrue();

        responderConnection.close();
    }

    @Test
    void testQuery_Timeout_ThrowsNatsQueryException() throws Exception {
        // Given - no responder set up, so request will timeout
        queryClient = new NatsQueryClient(natsUrl, objectMapper, 100, true); // Short timeout
        ExistsQueryRequest request = new ExistsQueryRequest("test-query-id", "officer", "BADGE-123");

        // When/Then
        assertThatThrownBy(() -> queryClient.query("query.officer.exists", request, ExistsQueryResponse.class))
                .isInstanceOf(NatsQueryClient.NatsQueryException.class)
                .hasMessageContaining("timed out");
    }

    @Test
    void testQuery_DisabledClient_ThrowsNatsQueryException() {
        // Given - client disabled
        queryClient = new NatsQueryClient(natsUrl, objectMapper, 5000, false);
        ExistsQueryRequest request = new ExistsQueryRequest("test-query-id", "officer", "BADGE-123");

        // When/Then
        assertThatThrownBy(() -> queryClient.query("query.officer.exists", request, ExistsQueryResponse.class))
                .isInstanceOf(NatsQueryClient.NatsQueryException.class)
                .hasMessageContaining("disabled or not connected");
    }

    @Test
    void testQuery_GeneratesQueryId_WhenMissing() throws Exception {
        // Given - set up responder
        String subject = "query.call.exists";
        Connection responderConnection = Nats.connect(natsUrl);
        Dispatcher dispatcher = responderConnection.createDispatcher(msg -> {
            try {
                ExistsQueryResponse response = new ExistsQueryResponse("generated-id", true);
                String responseJson = objectMapper.writeValueAsString(response);
                responderConnection.publish(msg.getReplyTo(), responseJson.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                // Ignore
            }
        });
        dispatcher.subscribe(subject);

        queryClient = new NatsQueryClient(natsUrl, objectMapper, 5000, true);
        ExistsQueryRequest request = new ExistsQueryRequest(null, "call", "CALL-123"); // No query ID

        // When
        ExistsQueryResponse response = queryClient.query(subject, request, ExistsQueryResponse.class);

        // Then - query ID should have been generated
        assertThat(request.getQueryId()).isNotNull();
        assertThat(request.getQueryId()).isNotEmpty();

        responderConnection.close();
    }

    @Test
    void testQuery_QueryFailure_ThrowsNatsQueryException() throws Exception {
        // Given - set up responder that returns error
        String subject = "query.incident.exists";
        Connection responderConnection = Nats.connect(natsUrl);
        Dispatcher dispatcher = responderConnection.createDispatcher(msg -> {
            try {
                ExistsQueryResponse response = new ExistsQueryResponse("test-id", "Database error");
                String responseJson = objectMapper.writeValueAsString(response);
                responderConnection.publish(msg.getReplyTo(), responseJson.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                // Ignore
            }
        });
        dispatcher.subscribe(subject);

        queryClient = new NatsQueryClient(natsUrl, objectMapper, 5000, true);
        ExistsQueryRequest request = new ExistsQueryRequest("test-id", "incident", "INC-123");

        // When/Then
        assertThatThrownBy(() -> queryClient.query(subject, request, ExistsQueryResponse.class))
                .isInstanceOf(NatsQueryClient.NatsQueryException.class)
                .hasMessageContaining("Query failed: Database error");

        responderConnection.close();
    }

    @Test
    void testQueryAsync_SuccessfulRequest_ReturnsFuture() throws Exception {
        // Given - set up responder
        String subject = "query.activity.exists";
        Connection responderConnection = Nats.connect(natsUrl);
        Dispatcher dispatcher = responderConnection.createDispatcher(msg -> {
            try {
                ExistsQueryResponse response = new ExistsQueryResponse("async-query-id", false);
                String responseJson = objectMapper.writeValueAsString(response);
                responderConnection.publish(msg.getReplyTo(), responseJson.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                // Ignore
            }
        });
        dispatcher.subscribe(subject);

        queryClient = new NatsQueryClient(natsUrl, objectMapper, 5000, true);
        ExistsQueryRequest request = new ExistsQueryRequest("async-query-id", "activity", "ACT-123");

        // When
        CompletableFuture<ExistsQueryResponse> future = queryClient.queryAsync(subject, request, ExistsQueryResponse.class);

        // Then
        ExistsQueryResponse response = future.get(5, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.getQueryId()).isEqualTo("async-query-id");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isExists()).isFalse();

        responderConnection.close();
    }

    @Test
    void testQueryAsync_Error_CompletesExceptionally() throws Exception {
        // Given - no responder, will timeout
        queryClient = new NatsQueryClient(natsUrl, objectMapper, 100, true); // Short timeout
        ExistsQueryRequest request = new ExistsQueryRequest("async-query-id", "assignment", "ASSIGN-123");

        // When
        CompletableFuture<ExistsQueryResponse> future = queryClient.queryAsync("query.assignment.exists", request, ExistsQueryResponse.class);

        // Then
        assertThatThrownBy(() -> future.get(2, TimeUnit.SECONDS))
                .hasCauseInstanceOf(NatsQueryClient.NatsQueryException.class);

        // Cleanup
        try {
            future.cancel(true);
        } catch (Exception e) {
            // Ignore
        }
    }

    @Test
    void testQueryAsync_DisabledClient_CompletesExceptionally() {
        // Given - client disabled
        queryClient = new NatsQueryClient(natsUrl, objectMapper, 5000, false);
        ExistsQueryRequest request = new ExistsQueryRequest("async-query-id", "dispatch", "DISP-123");

        // When
        CompletableFuture<ExistsQueryResponse> future = queryClient.queryAsync("query.dispatch.exists", request, ExistsQueryResponse.class);

        // Then
        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
                .hasCauseInstanceOf(NatsQueryClient.NatsQueryException.class)
                .hasMessageContaining("disabled or not connected");
    }

    @Test
    void testClose_ClosesConnection() throws Exception {
        // Given
        queryClient = new NatsQueryClient(natsUrl, objectMapper, 5000, true);

        // When
        queryClient.close();

        // Then - should not throw exception
        // Verify by trying to use it (should fail)
        ExistsQueryRequest request = new ExistsQueryRequest("test-id", "officer", "BADGE-123");
        assertThatThrownBy(() -> queryClient.query("query.officer.exists", request, ExistsQueryResponse.class))
                .isInstanceOf(NatsQueryClient.NatsQueryException.class);
    }

    @Test
    void testConstructor_WithMultipleServers_ConnectsToCluster() throws Exception {
        // Given - multiple server URLs (same server twice for testing)
        String multiServerUrl = natsUrl + "," + natsUrl;

        // When
        queryClient = new NatsQueryClient(multiServerUrl, objectMapper, 5000, true);

        // Then - should connect successfully
        assertThat(queryClient).isNotNull();
        
        // Verify it works
        String subject = "query.test.exists";
        Connection responderConnection = Nats.connect(natsUrl);
        Dispatcher dispatcher = responderConnection.createDispatcher(msg -> {
            try {
                ExistsQueryResponse response = new ExistsQueryResponse("test-id", true);
                String responseJson = objectMapper.writeValueAsString(response);
                responderConnection.publish(msg.getReplyTo(), responseJson.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                // Ignore
            }
        });
        dispatcher.subscribe(subject);

        ExistsQueryRequest request = new ExistsQueryRequest("test-id", "test", "RESOURCE-123");
        ExistsQueryResponse response = queryClient.query(subject, request, ExistsQueryResponse.class);
        assertThat(response.isSuccess()).isTrue();

        responderConnection.close();
    }

    @Test
    void testConstructor_WhenDisabled_DoesNotConnect() {
        // When - client disabled
        queryClient = new NatsQueryClient(natsUrl, objectMapper, 5000, false);

        // Then - should not throw exception
        assertThat(queryClient).isNotNull();
        
        // And query should fail with disabled message
        ExistsQueryRequest request = new ExistsQueryRequest("test-id", "officer", "BADGE-123");
        assertThatThrownBy(() -> queryClient.query("query.officer.exists", request, ExistsQueryResponse.class))
                .isInstanceOf(NatsQueryClient.NatsQueryException.class)
                .hasMessageContaining("disabled or not connected");
    }
}
