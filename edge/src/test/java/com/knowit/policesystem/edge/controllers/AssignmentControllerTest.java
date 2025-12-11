package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.assignments.ChangeAssignmentStatusRequested;
import com.knowit.policesystem.common.events.assignments.CompleteAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.LinkAssignmentToDispatchRequested;
import com.knowit.policesystem.common.events.resourceassignment.AssignResourceRequested;
import com.knowit.policesystem.common.events.resourceassignment.ChangeResourceAssignmentStatusRequested;
import com.knowit.policesystem.common.events.resourceassignment.UnassignResourceRequested;
import com.knowit.policesystem.edge.domain.AssignmentStatus;
import com.knowit.policesystem.edge.domain.AssignmentType;
import com.knowit.policesystem.edge.domain.ResourceType;
import com.knowit.policesystem.edge.domain.RoleType;
import com.knowit.policesystem.edge.domain.ResourceAssignmentStatus;
import com.knowit.policesystem.edge.dto.CreateAssignmentRequestDto;
import com.knowit.policesystem.edge.dto.CompleteAssignmentRequestDto;
import com.knowit.policesystem.edge.dto.ChangeAssignmentStatusRequestDto;
import com.knowit.policesystem.edge.dto.AssignResourceRequestDto;
import com.knowit.policesystem.edge.dto.UnassignResourceRequestDto;
import com.knowit.policesystem.edge.dto.ChangeResourceAssignmentStatusRequestDto;
import com.knowit.policesystem.edge.infrastructure.BaseIntegrationTest;
import com.knowit.policesystem.edge.infrastructure.NatsTestHelper;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AssignmentController.
 * Tests the full flow from REST API call to both Kafka and NATS/JetStream event production.
 */
class AssignmentControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;
    private Consumer<String, String> resourceAssignmentConsumer;
    private ObjectMapper eventObjectMapper;
    private NatsTestHelper natsHelper;
    private static final String TOPIC = "assignment-events";
    private static final String RESOURCE_ASSIGNMENT_TOPIC = "resource-assignment-events";

    @BeforeEach
    void setUp() throws Exception {
        // Configure ObjectMapper for event deserialization
        eventObjectMapper = new ObjectMapper();
        eventObjectMapper.registerModule(new JavaTimeModule());
        eventObjectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        eventObjectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Create Kafka consumer for verification
        // Use "latest" offset to only receive new messages published after subscription
        // This avoids conflicts with shared containers and is much faster
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(TOPIC));
        
        // Wait for partition assignment - consumer will start at latest offset automatically
        consumer.poll(Duration.ofSeconds(1));

        // Create Kafka consumer for resource assignment events with different group ID
        Properties resourceConsumerProps = new Properties();
        resourceConsumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        resourceConsumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-resource-consumer-group-" + System.currentTimeMillis());
        resourceConsumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        resourceConsumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        resourceConsumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        resourceAssignmentConsumer = new KafkaConsumer<>(resourceConsumerProps);
        resourceAssignmentConsumer.subscribe(Collections.singletonList(RESOURCE_ASSIGNMENT_TOPIC));
        
        // Wait for partition assignment - consumer will start at latest offset automatically
        resourceAssignmentConsumer.poll(Duration.ofSeconds(1));

        // Create NATS test helper
        natsHelper = new NatsTestHelper(nats.getNatsUrl(), eventObjectMapper);
        
        // Pre-create a catch-all stream for all command subjects to ensure it exists before publishing
        try {
            natsHelper.ensureStreamForSubject("commands.>");
        } catch (Exception e) {
            // Stream may already exist, continue
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (consumer != null) {
            consumer.close();
        }
        if (resourceAssignmentConsumer != null) {
            resourceAssignmentConsumer.close();
        }
        if (natsHelper != null) {
            natsHelper.close();
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

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        CreateAssignmentRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                CreateAssignmentRequested msgEvent = eventObjectMapper.readValue(msgJson, CreateAssignmentRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getEventType()).isEqualTo("CreateAssignmentRequested");
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

    @Test
    void testCompleteAssignment_WithValidData_ProducesEvent() throws Exception {
        // Given
        String assignmentId = "ASSIGN-010";
        Instant completedTime = Instant.now();
        CompleteAssignmentRequestDto request = new CompleteAssignmentRequestDto(completedTime);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/complete", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.message").value("Assignment completion request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(assignmentId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        CompleteAssignmentRequested event = eventObjectMapper.readValue(record.value(), CompleteAssignmentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(assignmentId);
        assertThat(event.getAssignmentId()).isEqualTo(assignmentId);
        assertThat(event.getCompletedTime()).isEqualTo(completedTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getEventType()).isEqualTo("CompleteAssignmentRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        CompleteAssignmentRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                CompleteAssignmentRequested msgEvent = eventObjectMapper.readValue(msgJson, CompleteAssignmentRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getEventType()).isEqualTo("CompleteAssignmentRequested");
    }

    @Test
    void testCompleteAssignment_WithMissingCompletedTime_Returns400() throws Exception {
        // Given - missing completedTime in request body
        String requestJson = """
                {
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/complete", "ASSIGN-011")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCompleteAssignment_WithNullCompletedTime_Returns400() throws Exception {
        // Given - null completedTime in request body
        String requestJson = """
                {
                    "completedTime": null
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/complete", "ASSIGN-012")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeAssignmentStatus_WithValidStatus_ProducesEvent() throws Exception {
        // Given
        String assignmentId = "ASSIGN-013";
        ChangeAssignmentStatusRequestDto request = new ChangeAssignmentStatusRequestDto(AssignmentStatus.InProgress);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/assignments/{assignmentId}/status", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.message").value("Assignment status change request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(assignmentId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        ChangeAssignmentStatusRequested event = eventObjectMapper.readValue(record.value(), ChangeAssignmentStatusRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(assignmentId);
        assertThat(event.getAssignmentId()).isEqualTo(assignmentId);
        assertThat(event.getStatus()).isEqualTo("In-Progress");
        assertThat(event.getEventType()).isEqualTo("ChangeAssignmentStatusRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        ChangeAssignmentStatusRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                ChangeAssignmentStatusRequested msgEvent = eventObjectMapper.readValue(msgJson, ChangeAssignmentStatusRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getEventType()).isEqualTo("ChangeAssignmentStatusRequested");

        // Test other status values
        testStatusConversion(assignmentId + "-1", AssignmentStatus.Created, "Created");
        testStatusConversion(assignmentId + "-2", AssignmentStatus.Assigned, "Assigned");
        testStatusConversion(assignmentId + "-3", AssignmentStatus.Completed, "Completed");
        testStatusConversion(assignmentId + "-4", AssignmentStatus.Cancelled, "Cancelled");
    }

    private void testStatusConversion(String assignmentId, AssignmentStatus status, String expectedStatusString) throws Exception {
        ChangeAssignmentStatusRequestDto request = new ChangeAssignmentStatusRequestDto(status);
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/assignments/{assignmentId}/status", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        ConsumerRecord<String, String> record = records.iterator().next();
        ChangeAssignmentStatusRequested event = eventObjectMapper.readValue(record.value(), ChangeAssignmentStatusRequested.class);
        assertThat(event.getStatus()).isEqualTo(expectedStatusString);
    }

    @Test
    void testChangeAssignmentStatus_WithMissingStatus_Returns400() throws Exception {
        // Given - missing status in request body
        String requestJson = """
                {
                }
                """;

        // When - call REST API
        mockMvc.perform(patch("/api/v1/assignments/{assignmentId}/status", "ASSIGN-014")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeAssignmentStatus_WithInvalidStatusEnum_Returns400() throws Exception {
        // Given - invalid status enum value
        String requestJson = """
                {
                    "status": "InvalidStatus"
                }
                """;

        // When - call REST API
        mockMvc.perform(patch("/api/v1/assignments/{assignmentId}/status", "ASSIGN-015")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkAssignmentToDispatch_WithValidData_ProducesEvent() throws Exception {
        // Given
        String assignmentId = "ASSIGN-016";
        String dispatchId = "DISP-001";
        com.knowit.policesystem.edge.dto.LinkAssignmentToDispatchRequestDto request =
                new com.knowit.policesystem.edge.dto.LinkAssignmentToDispatchRequestDto(dispatchId);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/dispatches", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Assignment link request processed"))
                .andExpect(jsonPath("$.data.assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.data.dispatchId").value(dispatchId));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(assignmentId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        com.knowit.policesystem.common.events.assignments.LinkAssignmentToDispatchRequested event =
                eventObjectMapper.readValue(record.value(), com.knowit.policesystem.common.events.assignments.LinkAssignmentToDispatchRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(assignmentId);
        assertThat(event.getAssignmentId()).isEqualTo(assignmentId);
        assertThat(event.getDispatchId()).isEqualTo(dispatchId);
        assertThat(event.getEventType()).isEqualTo("LinkAssignmentToDispatchRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        LinkAssignmentToDispatchRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                LinkAssignmentToDispatchRequested msgEvent = eventObjectMapper.readValue(msgJson, LinkAssignmentToDispatchRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getEventType()).isEqualTo("LinkAssignmentToDispatchRequested");
    }

    @Test
    void testLinkAssignmentToDispatch_WithMissingDispatchId_Returns400() throws Exception {
        // Given
        String assignmentId = "ASSIGN-017";
        com.knowit.policesystem.edge.dto.LinkAssignmentToDispatchRequestDto request =
                new com.knowit.policesystem.edge.dto.LinkAssignmentToDispatchRequestDto(null);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/dispatches", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkAssignmentToDispatch_WithBlankDispatchId_Returns400() throws Exception {
        // Given
        String assignmentId = "ASSIGN-018";
        com.knowit.policesystem.edge.dto.LinkAssignmentToDispatchRequestDto request =
                new com.knowit.policesystem.edge.dto.LinkAssignmentToDispatchRequestDto("   ");

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/dispatches", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testAssignResource_WithOfficer_ProducesEvent() throws Exception {
        // Given
        String assignmentId = "ASSIGN-019";
        String resourceId = "12345";
        Instant startTime = Instant.now();
        AssignResourceRequestDto request = new AssignResourceRequestDto(
                resourceId,
                ResourceType.Officer,
                RoleType.Primary,
                "Assigned",
                startTime
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/resources", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.resourceAssignmentId").exists())
                .andExpect(jsonPath("$.message").value("Resource assignment request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = resourceAssignmentConsumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(assignmentId);
        assertThat(record.topic()).isEqualTo(RESOURCE_ASSIGNMENT_TOPIC);

        // Deserialize and verify event data
        AssignResourceRequested event = eventObjectMapper.readValue(record.value(), AssignResourceRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(assignmentId);
        assertThat(event.getAssignmentId()).isEqualTo(assignmentId);
        assertThat(event.getResourceId()).isEqualTo(resourceId);
        assertThat(event.getResourceType()).isEqualTo("Officer");
        assertThat(event.getRoleType()).isEqualTo("Primary");
        assertThat(event.getStatus()).isEqualTo("Assigned");
        assertThat(event.getStartTime()).isEqualTo(startTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getEventType()).isEqualTo("AssignResourceRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        AssignResourceRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                AssignResourceRequested msgEvent = eventObjectMapper.readValue(msgJson, AssignResourceRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getEventType()).isEqualTo("AssignResourceRequested");
    }

    @Test
    void testAssignResource_WithVehicle_ProducesEvent() throws Exception {
        // Given
        String assignmentId = "ASSIGN-020";
        String resourceId = "UNIT-001";
        Instant startTime = Instant.now();
        AssignResourceRequestDto request = new AssignResourceRequestDto(
                resourceId,
                ResourceType.Vehicle,
                RoleType.Backup,
                "Assigned",
                startTime
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/resources", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.resourceAssignmentId").exists())
                .andExpect(jsonPath("$.message").value("Resource assignment request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = resourceAssignmentConsumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(assignmentId);
        assertThat(record.topic()).isEqualTo(RESOURCE_ASSIGNMENT_TOPIC);

        // Deserialize and verify event data
        AssignResourceRequested event = eventObjectMapper.readValue(record.value(), AssignResourceRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(assignmentId);
        assertThat(event.getAssignmentId()).isEqualTo(assignmentId);
        assertThat(event.getResourceId()).isEqualTo(resourceId);
        assertThat(event.getResourceType()).isEqualTo("Vehicle");
        assertThat(event.getRoleType()).isEqualTo("Backup");
        assertThat(event.getStatus()).isEqualTo("Assigned");
        assertThat(event.getStartTime()).isEqualTo(startTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getEventType()).isEqualTo("AssignResourceRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        AssignResourceRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                AssignResourceRequested msgEvent = eventObjectMapper.readValue(msgJson, AssignResourceRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getEventType()).isEqualTo("AssignResourceRequested");
    }

    @Test
    void testAssignResource_WithUnit_ProducesEvent() throws Exception {
        // Given
        String assignmentId = "ASSIGN-021";
        String resourceId = "UNIT-002";
        Instant startTime = Instant.now();
        AssignResourceRequestDto request = new AssignResourceRequestDto(
                resourceId,
                ResourceType.Unit,
                RoleType.Supervisor,
                "Assigned",
                startTime
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/resources", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.resourceAssignmentId").exists())
                .andExpect(jsonPath("$.message").value("Resource assignment request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = resourceAssignmentConsumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(assignmentId);
        assertThat(record.topic()).isEqualTo(RESOURCE_ASSIGNMENT_TOPIC);

        // Deserialize and verify event data
        AssignResourceRequested event = eventObjectMapper.readValue(record.value(), AssignResourceRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(assignmentId);
        assertThat(event.getAssignmentId()).isEqualTo(assignmentId);
        assertThat(event.getResourceId()).isEqualTo(resourceId);
        assertThat(event.getResourceType()).isEqualTo("Unit");
        assertThat(event.getRoleType()).isEqualTo("Supervisor");
        assertThat(event.getStatus()).isEqualTo("Assigned");
        assertThat(event.getStartTime()).isEqualTo(startTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getEventType()).isEqualTo("AssignResourceRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        AssignResourceRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                AssignResourceRequested msgEvent = eventObjectMapper.readValue(msgJson, AssignResourceRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getEventType()).isEqualTo("AssignResourceRequested");
    }

    @Test
    void testAssignResource_WithInvalidResourceType_Returns400() throws Exception {
        // Given - invalid resourceType enum value
        String requestJson = """
                {
                    "resourceId": "12345",
                    "resourceType": "InvalidType",
                    "roleType": "Primary",
                    "status": "Assigned"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/resources", "ASSIGN-022")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = resourceAssignmentConsumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testAssignResource_WithMissingRequiredFields_Returns400() throws Exception {
        // Given - missing resourceId
        String requestJsonMissingResourceId = """
                {
                    "resourceType": "Officer",
                    "roleType": "Primary",
                    "status": "Assigned"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/resources", "ASSIGN-023")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonMissingResourceId))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = resourceAssignmentConsumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();

        // Given - missing resourceType
        String requestJsonMissingResourceType = """
                {
                    "resourceId": "12345",
                    "roleType": "Primary",
                    "status": "Assigned"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/resources", "ASSIGN-024")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonMissingResourceType))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records2 = resourceAssignmentConsumer.poll(Duration.ofSeconds(2));
        assertThat(records2).isEmpty();

        // Given - missing roleType
        String requestJsonMissingRoleType = """
                {
                    "resourceId": "12345",
                    "resourceType": "Officer",
                    "status": "Assigned"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/resources", "ASSIGN-025")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonMissingRoleType))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records3 = resourceAssignmentConsumer.poll(Duration.ofSeconds(2));
        assertThat(records3).isEmpty();

        // Given - missing status
        String requestJsonMissingStatus = """
                {
                    "resourceId": "12345",
                    "resourceType": "Officer",
                    "roleType": "Primary"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/resources", "ASSIGN-026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonMissingStatus))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records4 = resourceAssignmentConsumer.poll(Duration.ofSeconds(2));
        assertThat(records4).isEmpty();
    }

    @Test
    void testUnassignResource_WithValidData_ProducesEvent() throws Exception {
        // Given
        String assignmentId = "ASSIGN-027";
        String resourceId = "12345";
        Instant endTime = Instant.now();
        UnassignResourceRequestDto request = new UnassignResourceRequestDto(endTime);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(delete("/api/v1/assignments/{assignmentId}/resources/{resourceId}", assignmentId, resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resourceAssignmentId").exists())
                .andExpect(jsonPath("$.message").value("Resource unassignment request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = resourceAssignmentConsumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(assignmentId);
        assertThat(record.topic()).isEqualTo(RESOURCE_ASSIGNMENT_TOPIC);

        // Deserialize and verify event data
        UnassignResourceRequested event = eventObjectMapper.readValue(record.value(), UnassignResourceRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(assignmentId);
        assertThat(event.getAssignmentId()).isEqualTo(assignmentId);
        assertThat(event.getResourceId()).isEqualTo(resourceId);
        assertThat(event.getEndTime()).isEqualTo(endTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getEventType()).isEqualTo("UnassignResourceRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        UnassignResourceRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                UnassignResourceRequested msgEvent = eventObjectMapper.readValue(msgJson, UnassignResourceRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getEventType()).isEqualTo("UnassignResourceRequested");
    }

    @Test
    void testUnassignResource_WithMissingEndTime_Returns400() throws Exception {
        // Given - missing endTime
        String requestJson = """
                {
                }
                """;

        // When - call REST API
        mockMvc.perform(delete("/api/v1/assignments/{assignmentId}/resources/{resourceId}", "ASSIGN-028", "12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = resourceAssignmentConsumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeResourceAssignmentStatus_WithValidStatus_ProducesEvent() throws Exception {
        // Given
        String assignmentId = "ASSIGN-029";
        String resourceId = "12345";
        ChangeResourceAssignmentStatusRequestDto request = new ChangeResourceAssignmentStatusRequestDto(ResourceAssignmentStatus.InProgress);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/assignments/{assignmentId}/resources/{resourceId}/status", assignmentId, resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resourceAssignmentId").exists())
                .andExpect(jsonPath("$.message").value("Resource assignment status change request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = resourceAssignmentConsumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(assignmentId);
        assertThat(record.topic()).isEqualTo(RESOURCE_ASSIGNMENT_TOPIC);

        // Deserialize and verify event data
        ChangeResourceAssignmentStatusRequested event = eventObjectMapper.readValue(record.value(), ChangeResourceAssignmentStatusRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(assignmentId);
        assertThat(event.getAssignmentId()).isEqualTo(assignmentId);
        assertThat(event.getResourceId()).isEqualTo(resourceId);
        assertThat(event.getStatus()).isEqualTo("In-Progress");
        assertThat(event.getEventType()).isEqualTo("ChangeResourceAssignmentStatusRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        ChangeResourceAssignmentStatusRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                ChangeResourceAssignmentStatusRequested msgEvent = eventObjectMapper.readValue(msgJson, ChangeResourceAssignmentStatusRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getEventType()).isEqualTo("ChangeResourceAssignmentStatusRequested");

        // Test other status values
        testResourceAssignmentStatusConversion(assignmentId + "-1", resourceId + "-1", ResourceAssignmentStatus.Assigned, "Assigned");
        testResourceAssignmentStatusConversion(assignmentId + "-2", resourceId + "-2", ResourceAssignmentStatus.Completed, "Completed");
        testResourceAssignmentStatusConversion(assignmentId + "-3", resourceId + "-3", ResourceAssignmentStatus.Cancelled, "Cancelled");
    }

    private void testResourceAssignmentStatusConversion(String assignmentId, String resourceId, ResourceAssignmentStatus status, String expectedStatusString) throws Exception {
        ChangeResourceAssignmentStatusRequestDto request = new ChangeResourceAssignmentStatusRequestDto(status);
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/assignments/{assignmentId}/resources/{resourceId}/status", assignmentId, resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        ConsumerRecords<String, String> records = resourceAssignmentConsumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        ConsumerRecord<String, String> record = records.iterator().next();
        ChangeResourceAssignmentStatusRequested event = eventObjectMapper.readValue(record.value(), ChangeResourceAssignmentStatusRequested.class);
        assertThat(event.getStatus()).isEqualTo(expectedStatusString);
    }

    @Test
    void testChangeResourceAssignmentStatus_WithMissingStatus_Returns400() throws Exception {
        // Given - missing status in request body
        String requestJson = """
                {
                }
                """;

        // When - call REST API
        mockMvc.perform(patch("/api/v1/assignments/{assignmentId}/resources/{resourceId}/status", "ASSIGN-030", "12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = resourceAssignmentConsumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeResourceAssignmentStatus_WithInvalidStatusEnum_Returns400() throws Exception {
        // Given - invalid status enum value
        String requestJson = """
                {
                    "status": "InvalidStatus"
                }
                """;

        // When - call REST API
        mockMvc.perform(patch("/api/v1/assignments/{assignmentId}/resources/{resourceId}/status", "ASSIGN-031", "12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = resourceAssignmentConsumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }
}
