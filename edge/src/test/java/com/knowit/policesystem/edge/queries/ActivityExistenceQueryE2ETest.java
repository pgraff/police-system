package com.knowit.policesystem.edge.queries;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.activities.StartActivityRequested;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.UpdateActivityRequestDto;
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
 * End-to-end integration tests for activity existence queries via NATS.
 * Tests the full flow: Edge → NATS → Projection → NATS → Edge
 */
class ActivityExistenceQueryE2ETest extends NatsQueryE2ETestBase {

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

        // Use consolidated operational-projection for activity domain
        boolean started = projectionContext.startProjection("activity");
        assertThat(started).as("Projection should start successfully").isTrue();

        boolean ready = projectionContext.waitForProjectionReady("activity", Duration.ofSeconds(30));
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
    void testExistsQuery_WhenActivityExists_ReturnsTrue() throws Exception {
        String activityId = "ACT-" + UUID.randomUUID();
        StartActivityRequested event = new StartActivityRequested(
                activityId,
                Instant.now(),
                "Interview",
                "Initial interview",
                "Started"
        );

        publishToKafkaAndWait(activityId, event);

        UpdateActivityRequestDto request = new UpdateActivityRequestDto("Updated description");

        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/activities/{activityId}", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activityId").value(activityId))
                .andExpect(jsonPath("$.message").value("Activity update request processed"));
    }

    @Test
    void testExistsQuery_WhenActivityDoesNotExist_ReturnsFalse() throws Exception {
        String nonExistentActivityId = "ACT-NONEXISTENT-" + UUID.randomUUID();

        UpdateActivityRequestDto request = new UpdateActivityRequestDto("Description");

        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/activities/{activityId}", nonExistentActivityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Activity not found: " + nonExistentActivityId));
    }

    @Test
    void testExistsQuery_WhenProjectionUnavailable_HandlesGracefully() throws Exception {
        projectionContext.stopProjection("activity");
        Thread.sleep(1000);

        String activityId = "ACT-" + UUID.randomUUID();
        UpdateActivityRequestDto request = new UpdateActivityRequestDto("Description");

        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/activities/{activityId}", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
    }

    private void publishToKafkaAndWait(String key, StartActivityRequested event) throws Exception {
        String payload = eventObjectMapper.writeValueAsString(event);
        kafkaProducer.send(new ProducerRecord<>(topicConfiguration.ACTIVITY_EVENTS, key, payload))
                .get(10, TimeUnit.SECONDS);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> true);
        
        Thread.sleep(2000);
    }
}

