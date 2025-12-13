package com.knowit.policesystem.edge.queries;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.domain.CallType;
import com.knowit.policesystem.edge.domain.Priority;
import com.knowit.policesystem.edge.dto.UpdateCallRequestDto;
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
 * End-to-end integration tests for call existence queries via NATS.
 * Tests the full flow: Edge → NATS → Projection → NATS → Edge
 */
class CallExistenceQueryE2ETest extends NatsQueryE2ETestBase {

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

        // Use consolidated operational-projection for call domain
        boolean started = projectionContext.startProjection("call");
        assertThat(started).as("Projection should start successfully").isTrue();

        boolean ready = projectionContext.waitForProjectionReady("call", Duration.ofSeconds(30));
        assertThat(ready).as("Projection should be ready").isTrue();
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
    void testExistsQuery_WhenCallExists_ReturnsTrue() throws Exception {
        String callId = "CALL-" + UUID.randomUUID();
        ReceiveCallRequested event = new ReceiveCallRequested(
                callId,
                "2024-CALL-001",
                "High",
                "Received",
                Instant.now(),
                "Emergency call",
                "Emergency"
        );

        publishToKafkaAndWait(callId, event);

        UpdateCallRequestDto request = new UpdateCallRequestDto(
                Priority.Medium,
                "Updated description",
                CallType.NonEmergency
        );

        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/calls/{callId}", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.callId").value(callId))
                .andExpect(jsonPath("$.message").value("Call update request processed"));
    }

    @Test
    void testExistsQuery_WhenCallDoesNotExist_ReturnsFalse() throws Exception {
        String nonExistentCallId = "CALL-NONEXISTENT-" + UUID.randomUUID();

        UpdateCallRequestDto request = new UpdateCallRequestDto(
                Priority.High,
                "Description",
                CallType.Emergency
        );

        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/calls/{callId}", nonExistentCallId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Call not found: " + nonExistentCallId));
    }

    @Test
    void testExistsQuery_WhenProjectionUnavailable_HandlesGracefully() throws Exception {
        projectionContext.stopProjection("call");
        Thread.sleep(1000);

        String callId = "CALL-" + UUID.randomUUID();
        UpdateCallRequestDto request = new UpdateCallRequestDto(
                Priority.High,
                "Description",
                CallType.Emergency
        );

        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/calls/{callId}", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
    }

    private void publishToKafkaAndWait(String key, ReceiveCallRequested event) throws Exception {
        String payload = eventObjectMapper.writeValueAsString(event);
        kafkaProducer.send(new ProducerRecord<>(topicConfiguration.CALL_EVENTS, key, payload))
                .get(10, TimeUnit.SECONDS);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> true);
        
        Thread.sleep(2000);
    }
}

