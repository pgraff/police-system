package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.activities.ChangeActivityStatusRequested;
import com.knowit.policesystem.common.events.activities.CompleteActivityRequested;
import com.knowit.policesystem.common.events.activities.LinkActivityToIncidentRequested;
import com.knowit.policesystem.common.events.activities.StartActivityRequested;
import com.knowit.policesystem.common.events.activities.UpdateActivityRequested;
import com.knowit.policesystem.edge.domain.ActivityStatus;
import com.knowit.policesystem.edge.domain.ActivityType;
import com.knowit.policesystem.edge.dto.ChangeActivityStatusRequestDto;
import com.knowit.policesystem.edge.dto.CompleteActivityRequestDto;
import com.knowit.policesystem.edge.dto.LinkActivityToIncidentRequestDto;
import com.knowit.policesystem.edge.dto.StartActivityRequestDto;
import com.knowit.policesystem.edge.dto.UpdateActivityRequestDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ActivityController.
 * Tests the full flow from REST API call to both Kafka and NATS/JetStream event production.
 */
class ActivityControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ActivityController activityController;

    private Consumer<String, String> consumer;
    private ObjectMapper eventObjectMapper;
    private NatsTestHelper natsHelper;
    private static final String TOPIC = "activity-events";

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
        if (natsHelper != null) {
            natsHelper.close();
        }
    }

    @Test
    void testStartActivity_WithValidData_ProducesEvent() throws Exception {
        // Given
        String activityId = "ACT-001";
        Instant activityTime = Instant.now();
        StartActivityRequestDto request = new StartActivityRequestDto(
                activityId,
                activityTime,
                ActivityType.Arrest,
                "Interviewing witness",
                ActivityStatus.InProgress
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.activityId").value(activityId))
                .andExpect(jsonPath("$.message").value("Activity start request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(activityId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        StartActivityRequested event = eventObjectMapper.readValue(record.value(), StartActivityRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(activityId);
        assertThat(event.getActivityId()).isEqualTo(activityId);
        assertThat(event.getActivityTime()).isEqualTo(activityTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getActivityType()).isEqualTo("Arrest");
        assertThat(event.getDescription()).isEqualTo("Interviewing witness");
        assertThat(event.getStatus()).isEqualTo("In-Progress");
        assertThat(event.getEventType()).isEqualTo("StartActivityRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        StartActivityRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                StartActivityRequested msgEvent = eventObjectMapper.readValue(msgJson, StartActivityRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("StartActivityRequested");
    }

    @Test
    void testStartActivity_WithMissingActivityId_Returns400() throws Exception {
        // Given
        StartActivityRequestDto request = new StartActivityRequestDto(
                null,  // Missing activityId
                Instant.now(),
                ActivityType.Interview,
                "Description",
                ActivityStatus.Started
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testStartActivity_WithInvalidEnum_Returns400() throws Exception {
        // Given - invalid activityType enum value
        String requestJsonWithInvalidType = """
                {
                    "activityId": "ACT-002",
                    "activityType": "InvalidType",
                    "status": "Started"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonWithInvalidType))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();

        // Given - invalid status enum value
        String requestJsonWithInvalidStatus = """
                {
                    "activityId": "ACT-003",
                    "activityType": "Arrest",
                    "status": "InvalidStatus"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonWithInvalidStatus))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records2 = consumer.poll(Duration.ofSeconds(2));
        assertThat(records2).isEmpty();
    }

    @Test
    void testCompleteActivity_WithValidData_ProducesEvent() throws Exception {
        // Given
        String activityId = "ACT-004";
        Instant completedTime = Instant.now();
        CompleteActivityRequestDto request = new CompleteActivityRequestDto(completedTime);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/activities/{activityId}/complete", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activityId").value(activityId))
                .andExpect(jsonPath("$.message").value("Activity completion request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(activityId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        CompleteActivityRequested event = eventObjectMapper.readValue(record.value(), CompleteActivityRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(activityId);
        assertThat(event.getActivityId()).isEqualTo(activityId);
        assertThat(event.getCompletedTime()).isEqualTo(completedTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getEventType()).isEqualTo("CompleteActivityRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        CompleteActivityRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                CompleteActivityRequested msgEvent = eventObjectMapper.readValue(msgJson, CompleteActivityRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("CompleteActivityRequested");
    }

    @Test
    void testCompleteActivity_WithMissingCompletedTime_Returns400() throws Exception {
        // Given - missing completedTime in request body
        String requestJson = """
                {
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/activities/{activityId}/complete", "ACT-005")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeActivityStatus_WithValidStatus_ProducesEvent() throws Exception {
        // Given
        String activityId = "ACT-006";
        ChangeActivityStatusRequestDto request = new ChangeActivityStatusRequestDto(ActivityStatus.InProgress);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/activities/{activityId}/status", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activityId").value(activityId))
                .andExpect(jsonPath("$.message").value("Activity status change request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(activityId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        ChangeActivityStatusRequested event = eventObjectMapper.readValue(record.value(), ChangeActivityStatusRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(activityId);
        assertThat(event.getActivityId()).isEqualTo(activityId);
        assertThat(event.getStatus()).isEqualTo("In-Progress");
        assertThat(event.getEventType()).isEqualTo("ChangeActivityStatusRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        ChangeActivityStatusRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                ChangeActivityStatusRequested msgEvent = eventObjectMapper.readValue(msgJson, ChangeActivityStatusRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("ChangeActivityStatusRequested");

        // Test other status values
        testStatusConversion(activityId + "-1", ActivityStatus.Started, "Started");
        testStatusConversion(activityId + "-2", ActivityStatus.Completed, "Completed");
        testStatusConversion(activityId + "-3", ActivityStatus.Cancelled, "Cancelled");
    }

    private void testStatusConversion(String activityId, ActivityStatus status, String expectedStatusString) throws Exception {
        ChangeActivityStatusRequestDto request = new ChangeActivityStatusRequestDto(status);
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/activities/{activityId}/status", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        ConsumerRecord<String, String> record = records.iterator().next();
        ChangeActivityStatusRequested event = eventObjectMapper.readValue(record.value(), ChangeActivityStatusRequested.class);
        assertThat(event.getStatus()).isEqualTo(expectedStatusString);
    }

    @Test
    void testChangeActivityStatus_WithMissingStatus_Returns400() throws Exception {
        // Given - missing status in request body
        String requestJson = """
                {
                }
                """;

        // When - call REST API
        mockMvc.perform(patch("/api/v1/activities/{activityId}/status", "ACT-007")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeActivityStatus_WithInvalidStatusEnum_Returns400() throws Exception {
        // Given - invalid status enum value
        String requestJson = """
                {
                    "status": "InvalidStatus"
                }
                """;

        // When - call REST API
        mockMvc.perform(patch("/api/v1/activities/{activityId}/status", "ACT-008")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateActivity_WithValidData_ProducesEvent() throws Exception {
        // Given
        String activityId = "ACT-009";
        String description = "Updated description for activity";
        UpdateActivityRequestDto request = new UpdateActivityRequestDto(description);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/activities/{activityId}", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activityId").value(activityId))
                .andExpect(jsonPath("$.message").value("Activity update request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(activityId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        UpdateActivityRequested event = eventObjectMapper.readValue(record.value(), UpdateActivityRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(activityId);
        assertThat(event.getActivityId()).isEqualTo(activityId);
        assertThat(event.getDescription()).isEqualTo(description);
        assertThat(event.getEventType()).isEqualTo("UpdateActivityRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        UpdateActivityRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                UpdateActivityRequested msgEvent = eventObjectMapper.readValue(msgJson, UpdateActivityRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("UpdateActivityRequested");
    }

    @Test
    void testUpdateActivity_WithBlankDescription_Returns400() throws Exception {
        // Given - blank description should be rejected
        String activityId = "ACT-010";
        String requestJson = """
                {
                    "description": "   "
                }
                """;

        // When - call REST API
        mockMvc.perform(put("/api/v1/activities/{activityId}", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkActivityToIncident_WithValidData_ProducesEvent() throws Exception {
        // Given
        String activityId = "ACT-011";
        String incidentId = "INC-001";

        LinkActivityToIncidentRequestDto request = new LinkActivityToIncidentRequestDto(incidentId);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/activities/{activityId}/incidents", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Activity link request processed"))
                .andExpect(jsonPath("$.data.activityId").value(activityId))
                .andExpect(jsonPath("$.data.incidentId").value(incidentId));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(activityId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        LinkActivityToIncidentRequested event = eventObjectMapper.readValue(record.value(), LinkActivityToIncidentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(activityId);
        assertThat(event.getActivityId()).isEqualTo(activityId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getEventType()).isEqualTo("LinkActivityToIncidentRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        LinkActivityToIncidentRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                LinkActivityToIncidentRequested msgEvent = eventObjectMapper.readValue(msgJson, LinkActivityToIncidentRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("LinkActivityToIncidentRequested");
    }

    @Test
    void testLinkActivityToIncident_WithMissingIncidentId_Returns400() throws Exception {
        // Given
        String activityId = "ACT-012";

        LinkActivityToIncidentRequestDto request = new LinkActivityToIncidentRequestDto(null);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/activities/{activityId}/incidents", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkActivityToIncident_WithBlankIncidentId_Returns400() throws Exception {
        // Given
        String activityId = "ACT-013";

        LinkActivityToIncidentRequestDto request = new LinkActivityToIncidentRequestDto("   ");

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/activities/{activityId}/incidents", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }
}
