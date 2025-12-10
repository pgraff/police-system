package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.officershifts.CheckInOfficerRequested;
import com.knowit.policesystem.common.events.officershifts.CheckOutOfficerRequested;
import com.knowit.policesystem.common.events.shifts.EndShiftRequested;
import com.knowit.policesystem.common.events.shifts.RecordShiftChangeRequested;
import com.knowit.policesystem.common.events.shifts.StartShiftRequested;
import com.knowit.policesystem.edge.domain.ChangeType;
import com.knowit.policesystem.edge.domain.ShiftRoleType;
import com.knowit.policesystem.edge.domain.ShiftStatus;
import com.knowit.policesystem.edge.domain.ShiftType;
import com.knowit.policesystem.edge.dto.CheckInOfficerRequestDto;
import com.knowit.policesystem.edge.dto.CheckOutOfficerRequestDto;
import com.knowit.policesystem.edge.dto.EndShiftRequestDto;
import com.knowit.policesystem.edge.dto.RecordShiftChangeRequestDto;
import com.knowit.policesystem.edge.dto.StartShiftRequestDto;
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
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for ShiftController.
 * Tests the flow from REST API call to Kafka event production.
 */
@SpringBootTest(classes = com.knowit.policesystem.edge.EdgeApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ShiftControllerTest {

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
    private Consumer<String, String> officerShiftConsumer;
    private ObjectMapper eventObjectMapper;
    private static final String TOPIC = "shift-events";
    private static final String OFFICER_SHIFT_TOPIC = "officer-shift-events";

    @BeforeEach
    void setUp() {
        eventObjectMapper = new ObjectMapper();
        eventObjectMapper.registerModule(new JavaTimeModule());
        eventObjectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        eventObjectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(TOPIC));

        consumer.poll(Duration.ofSeconds(1));

        ConsumerRecords<String, String> existingRecords;
        do {
            existingRecords = consumer.poll(Duration.ofMillis(100));
        } while (!existingRecords.isEmpty());

        // Set up consumer for officer-shift-events topic
        Properties officerShiftConsumerProps = new Properties();
        officerShiftConsumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        officerShiftConsumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-officer-shift-consumer-group-" + System.currentTimeMillis());
        officerShiftConsumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        officerShiftConsumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        officerShiftConsumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        officerShiftConsumer = new KafkaConsumer<>(officerShiftConsumerProps);
        officerShiftConsumer.subscribe(Collections.singletonList(OFFICER_SHIFT_TOPIC));

        officerShiftConsumer.poll(Duration.ofSeconds(1));

        ConsumerRecords<String, String> existingOfficerShiftRecords;
        do {
            existingOfficerShiftRecords = officerShiftConsumer.poll(Duration.ofMillis(100));
        } while (!existingOfficerShiftRecords.isEmpty());
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
        if (officerShiftConsumer != null) {
            officerShiftConsumer.close();
        }
    }

    @Test
    void testStartShift_WithValidData_ProducesEvent() throws Exception {
        String shiftId = "SHIFT-001";
        Instant startTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant endTime = startTime.plus(8, ChronoUnit.HOURS);

        StartShiftRequestDto request = new StartShiftRequestDto(
                shiftId,
                startTime,
                endTime,
                ShiftType.Day,
                ShiftStatus.Started
        );

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(requestJson)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.shiftId").value(shiftId))
                .andExpect(jsonPath("$.message").value("Shift start request created"));

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(shiftId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        StartShiftRequested event = eventObjectMapper.readValue(record.value(), StartShiftRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(shiftId);
        assertThat(event.getShiftId()).isEqualTo(shiftId);
        assertThat(event.getStartTime()).isEqualTo(startTime);
        assertThat(event.getEndTime()).isEqualTo(endTime);
        assertThat(event.getShiftType()).isEqualTo("Day");
        assertThat(event.getStatus()).isEqualTo("Started");
        assertThat(event.getEventType()).isEqualTo("StartShiftRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testStartShift_WithMissingShiftId_Returns400() throws Exception {
        StartShiftRequestDto request = new StartShiftRequestDto(
                null,
                Instant.now(),
                null,
                ShiftType.Day,
                ShiftStatus.Started
        );

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(requestJson)))
                .andExpect(status().isBadRequest());

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testStartShift_WithInvalidEnum_Returns400() throws Exception {
        String invalidEnumJson = """
                {
                    "shiftId": "SHIFT-002",
                    "shiftType": "InvalidType",
                    "status": "Started"
                }
                """;

        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(invalidEnumJson))
                .andExpect(status().isBadRequest());

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testStartShift_WithMissingRequiredFields_Returns400() throws Exception {
        // Missing shiftType
        String missingShiftTypeJson = """
                {
                    "shiftId": "SHIFT-003",
                    "status": "Started"
                }
                """;

        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(missingShiftTypeJson))
                .andExpect(status().isBadRequest());

        // Missing status
        String missingStatusJson = """
                {
                    "shiftId": "SHIFT-004",
                    "shiftType": "Day"
                }
                """;

        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(missingStatusJson))
                .andExpect(status().isBadRequest());

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testEndShift_WithValidData_ProducesEvent() throws Exception {
        String shiftId = "SHIFT-010";
        Instant endTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        EndShiftRequestDto request = new EndShiftRequestDto(endTime);
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/shifts/{shiftId}/end", shiftId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(requestJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shiftId").value(shiftId))
                .andExpect(jsonPath("$.message").value("Shift end request processed"));

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(shiftId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        EndShiftRequested event = eventObjectMapper.readValue(record.value(), EndShiftRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(shiftId);
        assertThat(event.getShiftId()).isEqualTo(shiftId);
        assertThat(event.getEndTime()).isEqualTo(endTime);
        assertThat(event.getEventType()).isEqualTo("EndShiftRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testEndShift_WithMissingEndTime_Returns400() throws Exception {
        EndShiftRequestDto request = new EndShiftRequestDto();
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/shifts/{shiftId}/end", "SHIFT-011")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(requestJson)))
                .andExpect(status().isBadRequest());

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeShiftStatus_WithValidStatus_ProducesEvent() throws Exception {
        String shiftId = "SHIFT-020";
        String statusJson = """
                {
                    "status": "InProgress"
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/shifts/{shiftId}/status", shiftId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(statusJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shiftId").value(shiftId))
                .andExpect(jsonPath("$.message").value("Shift status change request processed"));

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(shiftId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        com.knowit.policesystem.common.events.shifts.ChangeShiftStatusRequested event = 
                eventObjectMapper.readValue(record.value(), com.knowit.policesystem.common.events.shifts.ChangeShiftStatusRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(shiftId);
        assertThat(event.getShiftId()).isEqualTo(shiftId);
        assertThat(event.getStatus()).isEqualTo("In-Progress");
        assertThat(event.getEventType()).isEqualTo("ChangeShiftStatusRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testChangeShiftStatus_WithMissingStatus_Returns400() throws Exception {
        String shiftId = "SHIFT-021";
        String emptyJson = "{}";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/shifts/{shiftId}/status", shiftId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(emptyJson))
                .andExpect(status().isBadRequest());

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeShiftStatus_WithInvalidStatusEnum_Returns400() throws Exception {
        String shiftId = "SHIFT-022";
        String invalidStatusJson = """
                {
                    "status": "InvalidStatus"
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/shifts/{shiftId}/status", shiftId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(invalidStatusJson))
                .andExpect(status().isBadRequest());

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRecordShiftChange_WithValidData_ProducesEvent() throws Exception {
        String shiftId = "SHIFT-030";
        String shiftChangeId = "CHANGE-001";
        Instant changeTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        String notes = "Shift handoff notes";

        RecordShiftChangeRequestDto request = new RecordShiftChangeRequestDto(
                shiftChangeId,
                changeTime,
                ChangeType.Handoff,
                notes
        );
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/shifts/{shiftId}/shift-changes", shiftId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(requestJson)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.shiftChangeId").value(shiftChangeId))
                .andExpect(jsonPath("$.message").value("Shift change record request processed"));

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(shiftId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        RecordShiftChangeRequested event = eventObjectMapper.readValue(record.value(), RecordShiftChangeRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(shiftId);
        assertThat(event.getShiftId()).isEqualTo(shiftId);
        assertThat(event.getShiftChangeId()).isEqualTo(shiftChangeId);
        assertThat(event.getChangeTime()).isEqualTo(changeTime);
        assertThat(event.getChangeType()).isEqualTo("Handoff");
        assertThat(event.getNotes()).isEqualTo(notes);
        assertThat(event.getEventType()).isEqualTo("RecordShiftChangeRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testRecordShiftChange_WithMissingShiftChangeId_Returns400() throws Exception {
        String shiftId = "SHIFT-031";
        String missingShiftChangeIdJson = """
                {
                    "changeType": "Briefing"
                }
                """;

        mockMvc.perform(post("/api/v1/shifts/{shiftId}/shift-changes", shiftId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(missingShiftChangeIdJson))
                .andExpect(status().isBadRequest());

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRecordShiftChange_WithInvalidChangeType_Returns400() throws Exception {
        String shiftId = "SHIFT-032";
        String invalidChangeTypeJson = """
                {
                    "shiftChangeId": "CHANGE-002",
                    "changeType": "InvalidChangeType"
                }
                """;

        mockMvc.perform(post("/api/v1/shifts/{shiftId}/shift-changes", shiftId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(invalidChangeTypeJson))
                .andExpect(status().isBadRequest());

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCheckInOfficer_WithValidData_ProducesEvent() throws Exception {
        String shiftId = "SHIFT-040";
        String badgeNumber = "BADGE-001";
        Instant checkInTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        ShiftRoleType shiftRoleType = ShiftRoleType.Regular;

        CheckInOfficerRequestDto request = new CheckInOfficerRequestDto(checkInTime, shiftRoleType);
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/shifts/{shiftId}/officers/{badgeNumber}/check-in", shiftId, badgeNumber)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(requestJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shiftId").value(shiftId))
                .andExpect(jsonPath("$.data.badgeNumber").value(badgeNumber))
                .andExpect(jsonPath("$.message").value("Officer check-in request processed"));

        ConsumerRecords<String, String> records = officerShiftConsumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(shiftId);
        assertThat(record.topic()).isEqualTo(OFFICER_SHIFT_TOPIC);

        CheckInOfficerRequested event = eventObjectMapper.readValue(record.value(), CheckInOfficerRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(shiftId);
        assertThat(event.getShiftId()).isEqualTo(shiftId);
        assertThat(event.getBadgeNumber()).isEqualTo(badgeNumber);
        assertThat(event.getCheckInTime()).isEqualTo(checkInTime);
        assertThat(event.getShiftRoleType()).isEqualTo("Regular");
        assertThat(event.getEventType()).isEqualTo("CheckInOfficerRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testCheckInOfficer_WithMissingCheckInTime_Returns400() throws Exception {
        String shiftId = "SHIFT-041";
        String badgeNumber = "BADGE-002";
        String missingCheckInTimeJson = """
                {
                    "shiftRoleType": "Regular"
                }
                """;

        mockMvc.perform(post("/api/v1/shifts/{shiftId}/officers/{badgeNumber}/check-in", shiftId, badgeNumber)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(missingCheckInTimeJson))
                .andExpect(status().isBadRequest());

        ConsumerRecords<String, String> records = officerShiftConsumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCheckInOfficer_WithInvalidShiftRoleType_Returns400() throws Exception {
        String shiftId = "SHIFT-042";
        String badgeNumber = "BADGE-003";
        String invalidShiftRoleTypeJson = """
                {
                    "checkInTime": "2024-01-15T08:00:00Z",
                    "shiftRoleType": "InvalidRoleType"
                }
                """;

        mockMvc.perform(post("/api/v1/shifts/{shiftId}/officers/{badgeNumber}/check-in", shiftId, badgeNumber)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(invalidShiftRoleTypeJson))
                .andExpect(status().isBadRequest());

        ConsumerRecords<String, String> records = officerShiftConsumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCheckOutOfficer_WithValidData_ProducesEvent() throws Exception {
        String shiftId = "SHIFT-050";
        String badgeNumber = "BADGE-010";
        Instant checkOutTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        CheckOutOfficerRequestDto request = new CheckOutOfficerRequestDto(checkOutTime);
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/shifts/{shiftId}/officers/{badgeNumber}/check-out", shiftId, badgeNumber)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(requestJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shiftId").value(shiftId))
                .andExpect(jsonPath("$.data.badgeNumber").value(badgeNumber))
                .andExpect(jsonPath("$.message").value("Officer check-out request processed"));

        ConsumerRecords<String, String> records = officerShiftConsumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(shiftId);
        assertThat(record.topic()).isEqualTo(OFFICER_SHIFT_TOPIC);

        CheckOutOfficerRequested event = eventObjectMapper.readValue(record.value(), CheckOutOfficerRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(shiftId);
        assertThat(event.getShiftId()).isEqualTo(shiftId);
        assertThat(event.getBadgeNumber()).isEqualTo(badgeNumber);
        assertThat(event.getCheckOutTime()).isEqualTo(checkOutTime);
        assertThat(event.getEventType()).isEqualTo("CheckOutOfficerRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testCheckOutOfficer_WithMissingCheckOutTime_Returns400() throws Exception {
        String shiftId = "SHIFT-051";
        String badgeNumber = "BADGE-011";
        String missingCheckOutTimeJson = "{}";

        mockMvc.perform(post("/api/v1/shifts/{shiftId}/officers/{badgeNumber}/check-out", shiftId, badgeNumber)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(missingCheckOutTimeJson))
                .andExpect(status().isBadRequest());

        ConsumerRecords<String, String> records = officerShiftConsumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }
}
