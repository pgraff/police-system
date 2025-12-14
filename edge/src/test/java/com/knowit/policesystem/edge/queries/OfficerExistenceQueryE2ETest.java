package com.knowit.policesystem.edge.queries;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.domain.OfficerStatus;
import com.knowit.policesystem.edge.dto.UpdateOfficerRequestDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for officer existence queries via NATS.
 * Tests the full flow: Edge → NATS → Projection → NATS → Edge
 */
class OfficerExistenceQueryE2ETest extends NatsQueryE2ETestBase {

    private static final Logger log = LoggerFactory.getLogger(OfficerExistenceQueryE2ETest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TopicConfiguration topicConfiguration;

    @Autowired
    private com.knowit.policesystem.edge.services.projections.ProjectionQueryService projectionQueryService;

    private ProjectionTestContext projectionContext;
    private Producer<String, String> kafkaProducer;
    private ObjectMapper eventObjectMapper;
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws Exception {
        eventObjectMapper = new ObjectMapper();
        eventObjectMapper.registerModule(new JavaTimeModule());
        eventObjectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Create JdbcTemplate for database polling
        org.springframework.jdbc.datasource.SimpleDriverDataSource dataSource = 
            new org.springframework.jdbc.datasource.SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        dataSource.setUrl(getPostgresJdbcUrl());
        dataSource.setUsername(getPostgresUsername());
        dataSource.setPassword(getPostgresPassword());
        jdbcTemplate = new org.springframework.jdbc.core.JdbcTemplate(dataSource);

        // Create Kafka producer for setting up test data
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProducer = new KafkaProducer<>(producerProps);

        // Start projection service
        projectionContext = new ProjectionTestContext(
                nats.getNatsUrl(),
                kafka.getBootstrapServers(),
                getPostgresJdbcUrl(),
                getPostgresUsername(),
                getPostgresPassword()
        );

        // Use consolidated resource-projection for officer domain
        boolean started = projectionContext.startProjection("officer");
        assertThat(started).as("Projection should start successfully").isTrue();

        // Wait for projection to be ready
        boolean ready = projectionContext.waitForProjectionReady("officer", Duration.ofSeconds(30));
        assertThat(ready).as("Projection should be ready").isTrue();
        
        // Give projection additional time to initialize Kafka consumer and connect
        // This ensures the consumer is subscribed before we publish events
        Thread.sleep(5000);
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
    void testExistsQuery_WhenOfficerExists_ReturnsTrue() throws Exception {
        // Given - create an officer in the projection via Kafka event
        String badgeNumber = "BADGE-" + UUID.randomUUID();
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

        // Publish event to Kafka and wait for projection to process
        publishToKafkaAndWait(badgeNumber, event);

        // When - call edge API that triggers existence check (update officer)
        UpdateOfficerRequestDto request = new UpdateOfficerRequestDto(
                "Jane",
                "Smith",
                "Sergeant",
                "jane.smith@police.gov",
                "555-0200",
                LocalDate.of(2021, 3, 20)
        );

        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/officers/{badgeNumber}", badgeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.badgeNumber").value(badgeNumber))
                .andExpect(jsonPath("$.message").value("Officer update request processed"));
    }

    @Test
    void testExistsQuery_WhenOfficerDoesNotExist_ReturnsFalse() throws Exception {
        // Given - officer doesn't exist (no event published)
        String nonExistentBadgeNumber = "BADGE-NONEXISTENT-" + UUID.randomUUID();

        // When - call edge API that triggers existence check
        UpdateOfficerRequestDto request = new UpdateOfficerRequestDto(
                "John",
                "Doe",
                "Officer",
                "john.doe@police.gov",
                "555-0100",
                LocalDate.of(2020, 1, 15)
        );

        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/officers/{badgeNumber}", nonExistentBadgeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Officer not found: " + nonExistentBadgeNumber));
    }

    @Test
    void testExistsQuery_WhenProjectionUnavailable_HandlesGracefully() throws Exception {
        // Given - stop the projection service
        projectionContext.stopProjection("officer");

        // Wait a bit for the connection to be closed
        Thread.sleep(1000);

        // When - call edge API
        String badgeNumber = "BADGE-" + UUID.randomUUID();
        UpdateOfficerRequestDto request = new UpdateOfficerRequestDto(
                "John",
                "Doe",
                "Officer",
                "john.doe@police.gov",
                "555-0100",
                LocalDate.of(2020, 1, 15)
        );

        String requestJson = objectMapper.writeValueAsString(request);
        // The edge should handle the connection failure gracefully
        // It may return 500 or default to false (which causes 404)
        mockMvc.perform(put("/api/v1/officers/{badgeNumber}", badgeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound()); // Default behavior is to return false, causing 404
    }

    /**
     * Publishes an event to Kafka and waits for the projection to process it.
     */
    private void publishToKafkaAndWait(String key, RegisterOfficerRequested event) throws Exception {
        String payload = eventObjectMapper.writeValueAsString(event);
        kafkaProducer.send(new ProducerRecord<>(topicConfiguration.OFFICER_EVENTS, key, payload))
                .get(10, TimeUnit.SECONDS);

        // Wait for projection to process the event by polling the database
        // Give it more time as the projection needs to start, connect to Kafka, and process the event
        log.info("Published event for officer {} to Kafka, waiting for projection to process...", key);
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(1000))
                .until(() -> {
                    try {
                        // Use the projection query service to check existence
                        // This tests the actual query path (Edge → NATS → Projection → NATS → Edge)
                        boolean exists = projectionQueryService.exists("officer", key);
                        if (exists) {
                            log.info("Officer {} found via projection query service!", key);
                        } else {
                            log.debug("Waiting for officer {} to appear in projection (checked via query service)...", key);
                        }
                        return exists;
                    } catch (Exception e) {
                        log.warn("Error checking projection for officer {}: {}", key, e.getMessage());
                        return false;
                    }
                });
    }
}

