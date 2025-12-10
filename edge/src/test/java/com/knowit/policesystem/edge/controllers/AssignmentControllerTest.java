package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.edge.domain.AssignmentStatus;
import com.knowit.policesystem.edge.domain.AssignmentType;
import com.knowit.policesystem.edge.dto.CreateAssignmentRequestDto;
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
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AssignmentController.
 * Tests the full flow from REST API call to Kafka event production.
 */
@SpringBootTest(classes = com.knowit.policesystem.edge.EdgeApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AssignmentControllerTest {

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
    private static final String TOPIC = "assignment-events";

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
    void testCreateAssignment_WithIncidentId_ProducesEvent() throws Exception {
        // Given
        String assignmentId = "ASSIGN-001";
        Instant assignedTime = Instant.now();
        CreateAssignmentRequestDto request = new CreateAssignmentRequestDto(
                assignmentId,
                assignedTime,
                AssignmentType.Primary,
                AssignmentStatus.Created,
                "INC-001",
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.message").value("Assignment creation request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(assignmentId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        CreateAssignmentRequested event = eventObjectMapper.readValue(record.value(), CreateAssignmentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(assignmentId);
        assertThat(event.getAssignmentId()).isEqualTo(assignmentId);
        assertThat(event.getAssignedTime()).isEqualTo(assignedTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getAssignmentType()).isEqualTo("Primary");
        assertThat(event.getStatus()).isEqualTo("Created");
        assertThat(event.getIncidentId()).isEqualTo("INC-001");
        assertThat(event.getCallId()).isNull();
        assertThat(event.getEventType()).isEqualTo("CreateAssignmentRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testCreateAssignment_WithCallId_ProducesEvent() throws Exception {
        // Given
        String assignmentId = "ASSIGN-002";
        Instant assignedTime = Instant.now();
        CreateAssignmentRequestDto request = new CreateAssignmentRequestDto(
                assignmentId,
                assignedTime,
                AssignmentType.Backup,
                AssignmentStatus.Assigned,
                null,
                "CALL-001"
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.message").value("Assignment creation request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(assignmentId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        CreateAssignmentRequested event = eventObjectMapper.readValue(record.value(), CreateAssignmentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(assignmentId);
        assertThat(event.getAssignmentId()).isEqualTo(assignmentId);
        assertThat(event.getAssignedTime()).isEqualTo(assignedTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getAssignmentType()).isEqualTo("Backup");
        assertThat(event.getStatus()).isEqualTo("Assigned");
        assertThat(event.getIncidentId()).isNull();
        assertThat(event.getCallId()).isEqualTo("CALL-001");
        assertThat(event.getEventType()).isEqualTo("CreateAssignmentRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testCreateAssignment_WithBothIncidentAndCall_Returns400() throws Exception {
        // Given - both incidentId and callId provided (should fail)
        String requestJson = """
                {
                    "assignmentId": "ASSIGN-003",
                    "assignmentType": "Primary",
                    "status": "Created",
                    "incidentId": "INC-001",
                    "callId": "CALL-001"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCreateAssignment_WithNeitherIncidentNorCall_Returns400() throws Exception {
        // Given - neither incidentId nor callId provided (should fail)
        String requestJson = """
                {
                    "assignmentId": "ASSIGN-004",
                    "assignmentType": "Primary",
                    "status": "Created"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCreateAssignment_WithInvalidEnums_Returns400() throws Exception {
        // Given - invalid assignmentType enum value
        String requestJsonWithInvalidType = """
                {
                    "assignmentId": "ASSIGN-005",
                    "assignmentType": "InvalidType",
                    "status": "Created",
                    "incidentId": "INC-001"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonWithInvalidType))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();

        // Given - invalid status enum value
        String requestJsonWithInvalidStatus = """
                {
                    "assignmentId": "ASSIGN-006",
                    "assignmentType": "Primary",
                    "status": "InvalidStatus",
                    "incidentId": "INC-001"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonWithInvalidStatus))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records2 = consumer.poll(Duration.ofSeconds(2));
        assertThat(records2).isEmpty();
    }

    @Test
    void testCreateAssignment_WithMissingRequiredFields_Returns400() throws Exception {
        // Given - missing assignmentId
        String requestJsonMissingId = """
                {
                    "assignmentType": "Primary",
                    "status": "Created",
                    "incidentId": "INC-001"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonMissingId))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();

        // Given - missing assignmentType
        String requestJsonMissingType = """
                {
                    "assignmentId": "ASSIGN-007",
                    "status": "Created",
                    "incidentId": "INC-001"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonMissingType))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records2 = consumer.poll(Duration.ofSeconds(2));
        assertThat(records2).isEmpty();

        // Given - missing status
        String requestJsonMissingStatus = """
                {
                    "assignmentId": "ASSIGN-008",
                    "assignmentType": "Primary",
                    "incidentId": "INC-001"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonMissingStatus))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records3 = consumer.poll(Duration.ofSeconds(2));
        assertThat(records3).isEmpty();
    }

    @Test
    void testCreateAssignment_WithInProgressStatus_ConvertsToHyphenatedString() throws Exception {
        // Given
        String assignmentId = "ASSIGN-009";
        Instant assignedTime = Instant.now();
        CreateAssignmentRequestDto request = new CreateAssignmentRequestDto(
                assignmentId,
                assignedTime,
                AssignmentType.Supervisor,
                AssignmentStatus.InProgress,
                "INC-001",
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.assignmentId").value(assignmentId));

        // Then - verify event in Kafka with converted status
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        CreateAssignmentRequested event = eventObjectMapper.readValue(record.value(), CreateAssignmentRequested.class);
        assertThat(event.getStatus()).isEqualTo("In-Progress");
    }
}
