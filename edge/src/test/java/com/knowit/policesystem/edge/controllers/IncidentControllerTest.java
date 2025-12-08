package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.edge.domain.IncidentStatus;
import com.knowit.policesystem.edge.domain.IncidentType;
import com.knowit.policesystem.edge.domain.Priority;
import com.knowit.policesystem.edge.dto.ReportIncidentRequestDto;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for IncidentController.
 * Tests the full flow from REST API call to Kafka event production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class IncidentControllerTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:latest")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;
    private ObjectMapper eventObjectMapper;
    private static final String TOPIC = "incident-events";

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper for event deserialization
        eventObjectMapper = new ObjectMapper();
        eventObjectMapper.registerModule(new JavaTimeModule());
        eventObjectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        eventObjectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Create Kafka consumer for verification
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(TOPIC));
        
        // Wait for partition assignment and consume any existing events
        consumer.poll(Duration.ofSeconds(1));
        
        // Consume and discard all existing events to start fresh
        ConsumerRecords<String, String> existingRecords;
        do {
            existingRecords = consumer.poll(Duration.ofMillis(100));
        } while (!existingRecords.isEmpty());
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void testReportIncident_WithValidData_ProducesEvent() throws Exception {
        // Given
        String incidentId = "INC-001";
        String incidentNumber = "2024-001";
        Instant reportedTime = Instant.now();
        ReportIncidentRequestDto request = new ReportIncidentRequestDto(
                incidentId,
                incidentNumber,
                Priority.High,
                IncidentStatus.Reported,
                reportedTime,
                "Traffic accident at Main St and Oak Ave",
                IncidentType.Traffic
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.incidentId").value(incidentId))
                .andExpect(jsonPath("$.data.incidentNumber").value(incidentNumber))
                .andExpect(jsonPath("$.message").value("Incident report request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(incidentId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        ReportIncidentRequested event = eventObjectMapper.readValue(record.value(), ReportIncidentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(incidentId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getIncidentNumber()).isEqualTo(incidentNumber);
        assertThat(event.getPriority()).isEqualTo("High");
        assertThat(event.getStatus()).isEqualTo("Reported");
        assertThat(event.getDescription()).isEqualTo("Traffic accident at Main St and Oak Ave");
        assertThat(event.getIncidentType()).isEqualTo("Traffic");
        assertThat(event.getEventType()).isEqualTo("ReportIncidentRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testReportIncident_WithMissingIncidentId_Returns400() throws Exception {
        // Given
        ReportIncidentRequestDto request = new ReportIncidentRequestDto(
                null,  // Missing incidentId
                "2024-001",
                Priority.High,
                IncidentStatus.Reported,
                Instant.now(),
                "Description",
                IncidentType.Traffic
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testReportIncident_WithInvalidPriority_Returns400() throws Exception {
        // Given - invalid priority enum value
        String requestJson = """
                {
                    "incidentId": "INC-002",
                    "incidentNumber": "2024-002",
                    "priority": "InvalidPriority",
                    "status": "Reported",
                    "incidentType": "Traffic"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testReportIncident_WithInvalidStatus_Returns400() throws Exception {
        // Given - invalid status enum value
        String requestJson = """
                {
                    "incidentId": "INC-003",
                    "incidentNumber": "2024-003",
                    "priority": "High",
                    "status": "InvalidStatus",
                    "incidentType": "Traffic"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testReportIncident_WithInvalidIncidentType_Returns400() throws Exception {
        // Given - invalid incidentType enum value
        String requestJson = """
                {
                    "incidentId": "INC-004",
                    "incidentNumber": "2024-004",
                    "priority": "High",
                    "status": "Reported",
                    "incidentType": "InvalidType"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testReportIncident_WithEmptyIncidentId_Returns400() throws Exception {
        // Given
        ReportIncidentRequestDto request = new ReportIncidentRequestDto(
                "",  // Empty incidentId
                "2024-001",
                Priority.High,
                IncidentStatus.Reported,
                Instant.now(),
                "Description",
                IncidentType.Traffic
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }
}
