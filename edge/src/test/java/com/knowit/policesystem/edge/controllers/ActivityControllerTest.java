package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.activities.ChangeActivityStatusRequested;
import com.knowit.policesystem.common.events.activities.CompleteActivityRequested;
import com.knowit.policesystem.common.events.activities.StartActivityRequested;
import com.knowit.policesystem.edge.domain.ActivityStatus;
import com.knowit.policesystem.edge.domain.ActivityType;
import com.knowit.policesystem.edge.dto.ChangeActivityStatusRequestDto;
import com.knowit.policesystem.edge.dto.CompleteActivityRequestDto;
import com.knowit.policesystem.edge.dto.StartActivityRequestDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ActivityController.
 * Tests the full flow from REST API call to Kafka event production.
 */
@SpringBootTest(classes = com.knowit.policesystem.edge.EdgeApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ActivityControllerTest {

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

    @Autowired
    private ActivityController activityController;

    private Consumer<String, String> consumer;
    private ObjectMapper eventObjectMapper;
    private static final String TOPIC = "activity-events";

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
}
