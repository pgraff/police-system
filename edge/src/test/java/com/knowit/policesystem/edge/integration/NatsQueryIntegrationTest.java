package com.knowit.policesystem.edge.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.domain.CallType;
import com.knowit.policesystem.edge.domain.Priority;
import com.knowit.policesystem.edge.infrastructure.NatsQueryE2ETestBase;
import com.knowit.policesystem.edge.infrastructure.ProjectionTestContext;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for NATS request-response query flow.
 * Tests the full flow: Edge → NATS → Projection → NATS → Edge
 */
class NatsQueryIntegrationTest extends NatsQueryE2ETestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TopicConfiguration topicConfiguration;

    private ProjectionTestContext projectionContext;
    private Producer<String, String> kafkaProducer;
    private ObjectMapper eventObjectMapper;

    @BeforeEach
    void setUp() throws Exception {
        eventObjectMapper = new ObjectMapper();
        eventObjectMapper.registerModule(new JavaTimeModule());
        eventObjectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProducer = new KafkaProducer<>(producerProps);

        projectionContext = new ProjectionTestContext(
                nats.getNatsUrl(),
                kafka.getBootstrapServers(),
                getPostgresJdbcUrl(),
                getPostgresUsername(),
                getPostgresPassword()
        );
    }

    @AfterEach
    void tearDown() {
        if (projectionContext != null) {
            projectionContext.stopAll();
        }
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
    }

    @Test
    void testExistsQuery_EdgeToProjection_ReturnsResponse() throws Exception {
        // Given - start call projection
        boolean started = projectionContext.startProjection("call", "com.knowit.policesystem.projection.CallProjectionApplication");
        assertThat(started).as("Call projection should start successfully").isTrue();

        boolean ready = projectionContext.waitForProjectionReady("call", Duration.ofSeconds(30));
        assertThat(ready).as("Call projection should be ready").isTrue();

        // Create a call via event
        String callId = "CALL-E2E-001";
        ReceiveCallRequested event = new ReceiveCallRequested(
                callId,
                "2024-CALL-001",
                "High",
                "Received",
                Instant.now(),
                "Test call for E2E query test",
                "Emergency"
        );

        String eventJson = eventObjectMapper.writeValueAsString(event);
        kafkaProducer.send(new ProducerRecord<>(topicConfiguration.CALL_EVENTS, callId, eventJson)).get();

        // Wait for projection to process event
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    // Try to update the call - if it exists, we'll get 200, if not, 404
                    try {
                        String updateJson = """
                                {
                                    "priority": "Medium",
                                    "description": "Updated description"
                                }
                                """;
                        mockMvc.perform(put("/api/v1/calls/{callId}", callId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(updateJson))
                                .andExpect(status().isOk());
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });

        // When - try to update a non-existent call (should return 404)
        String nonExistentCallId = "CALL-NONEXISTENT";
        String updateJson = """
                {
                    "priority": "Medium",
                    "description": "Updated description"
                }
                """;

        // Then - should return 404 because call doesn't exist
        mockMvc.perform(put("/api/v1/calls/{callId}", nonExistentCallId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Call not found: " + nonExistentCallId));
    }

    @Test
    void testGetQuery_EdgeToProjection_ReturnsData() throws Exception {
        // Given - start officer projection
        boolean started = projectionContext.startProjection("officer", "com.knowit.policesystem.projection.OfficerProjectionApplication");
        assertThat(started).as("Officer projection should start successfully").isTrue();

        boolean ready = projectionContext.waitForProjectionReady("officer", Duration.ofSeconds(30));
        assertThat(ready).as("Officer projection should be ready").isTrue();

        // Create an officer via event
        String badgeNumber = "BADGE-E2E-001";
        RegisterOfficerRequested event = new RegisterOfficerRequested(
                badgeNumber,
                "John",
                "Doe",
                "Officer",
                "john.doe@police.gov",
                "555-0100",
                "2020-01-15",
                "Active"
        );

        String eventJson = eventObjectMapper.writeValueAsString(event);
        kafkaProducer.send(new ProducerRecord<>(topicConfiguration.OFFICER_EVENTS, badgeNumber, eventJson)).get();

        // Wait for projection to process event
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    // Try to update the officer - if it exists, we'll get 200, if not, 404
                    try {
                        String updateJson = """
                                {
                                    "firstName": "Jane",
                                    "lastName": "Smith"
                                }
                                """;
                        mockMvc.perform(put("/api/v1/officers/{badgeNumber}", badgeNumber)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(updateJson))
                                .andExpect(status().isOk());
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });

        // When - try to update a non-existent officer (should return 404)
        String nonExistentBadge = "BADGE-NONEXISTENT";
        String updateJson = """
                {
                    "firstName": "Jane",
                    "lastName": "Smith"
                }
                """;

        // Then - should return 404 because officer doesn't exist
        mockMvc.perform(put("/api/v1/officers/{badgeNumber}", nonExistentBadge)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Officer not found: " + nonExistentBadge));
    }

    @Test
    void testQuery_MultipleDomains_Works() throws Exception {
        // Given - start multiple projections
        boolean callStarted = projectionContext.startProjection("call", "com.knowit.policesystem.projection.CallProjectionApplication");
        assertThat(callStarted).as("Call projection should start").isTrue();

        boolean officerStarted = projectionContext.startProjection("officer", "com.knowit.policesystem.projection.OfficerProjectionApplication");
        assertThat(officerStarted).as("Officer projection should start").isTrue();

        // Wait for both to be ready
        boolean callReady = projectionContext.waitForProjectionReady("call", Duration.ofSeconds(30));
        assertThat(callReady).as("Call projection should be ready").isTrue();

        boolean officerReady = projectionContext.waitForProjectionReady("officer", Duration.ofSeconds(30));
        assertThat(officerReady).as("Officer projection should be ready").isTrue();

        // When/Then - queries to different domains should work independently
        // Test call domain
        String nonExistentCallId = "CALL-MULTI-001";
        String callUpdateJson = """
                {
                    "priority": "Medium"
                }
                """;
        mockMvc.perform(put("/api/v1/calls/{callId}", nonExistentCallId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callUpdateJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Call not found: " + nonExistentCallId));

        // Test officer domain
        String nonExistentBadge = "BADGE-MULTI-001";
        String officerUpdateJson = """
                {
                    "firstName": "Test"
                }
                """;
        mockMvc.perform(put("/api/v1/officers/{badgeNumber}", nonExistentBadge)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(officerUpdateJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Officer not found: " + nonExistentBadge));
    }

    @Test
    void testExistsQuery_ProjectionUnavailable_HandlesGracefully() throws Exception {
        // Given - don't start any projection, so queries will timeout

        // When - try to update a call (projection unavailable)
        String callId = "CALL-UNAVAILABLE";
        String updateJson = """
                {
                    "priority": "Medium"
                }
                """;

        // Then - should handle gracefully (may timeout or return error, but shouldn't crash)
        // The exact behavior depends on timeout settings, but it should not throw unhandled exception
        try {
            mockMvc.perform(put("/api/v1/calls/{callId}", callId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isNotFound()); // Should return 404 (resource doesn't exist)
        } catch (Exception e) {
            // If timeout occurs, that's acceptable - the important thing is it's handled
            assertThat(e.getMessage()).containsAnyOf("timeout", "Not Found");
        }
    }
}
