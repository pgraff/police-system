package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.shifts.StartShiftRequested;
import com.knowit.policesystem.edge.domain.ShiftStatus;
import com.knowit.policesystem.edge.domain.ShiftType;
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
    private ObjectMapper eventObjectMapper;
    private static final String TOPIC = "shift-events";

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
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
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
}
