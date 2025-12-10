package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.calls.ArriveAtCallRequested;
import com.knowit.policesystem.common.events.calls.ClearCallRequested;
import com.knowit.policesystem.common.events.calls.ChangeCallStatusRequested;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.calls.DispatchCallRequested;
import com.knowit.policesystem.edge.domain.CallStatus;
import com.knowit.policesystem.edge.domain.CallType;
import com.knowit.policesystem.edge.domain.Priority;
import com.knowit.policesystem.edge.dto.ArriveAtCallRequestDto;
import com.knowit.policesystem.edge.dto.ChangeCallStatusRequestDto;
import com.knowit.policesystem.edge.dto.ClearCallRequestDto;
import com.knowit.policesystem.edge.dto.DispatchCallRequestDto;
import com.knowit.policesystem.edge.dto.ReceiveCallRequestDto;
import com.knowit.policesystem.edge.services.calls.CallExistenceService;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CallController.
 * Tests the full flow from REST API call to Kafka event production.
 */
@SpringBootTest(classes = com.knowit.policesystem.edge.EdgeApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(CallControllerTest.TestCallExistenceConfig.class)
class CallControllerTest {

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
    private CallController callController;

    @Autowired
    private InMemoryCallExistenceService callExistenceService;

    private Consumer<String, String> consumer;
    private ObjectMapper eventObjectMapper;
    private static final String TOPIC = "call-events";

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

        callExistenceService.clear();

    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void testReceiveCall_WithValidData_ProducesEvent() throws Exception {
        // Given
        String callId = "CALL-001";
        String callNumber = "2024-CALL-001";
        Instant receivedTime = Instant.now();
        ReceiveCallRequestDto request = new ReceiveCallRequestDto(
                callId,
                callNumber,
                Priority.High,
                CallStatus.Received,
                receivedTime,
                "Emergency call from Main St",
                CallType.Emergency
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.callId").value(callId))
                .andExpect(jsonPath("$.data.callNumber").value(callNumber))
                .andExpect(jsonPath("$.message").value("Call receive request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(callId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        ReceiveCallRequested event = eventObjectMapper.readValue(record.value(), ReceiveCallRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(callId);
        assertThat(event.getCallId()).isEqualTo(callId);
        assertThat(event.getCallNumber()).isEqualTo(callNumber);
        assertThat(event.getPriority()).isEqualTo("High");
        assertThat(event.getStatus()).isEqualTo("Received");
        assertThat(event.getDescription()).isEqualTo("Emergency call from Main St");
        assertThat(event.getCallType()).isEqualTo("Emergency");
        assertThat(event.getEventType()).isEqualTo("ReceiveCallRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testReceiveCall_WithMissingCallId_Returns400() throws Exception {
        // Given
        ReceiveCallRequestDto request = new ReceiveCallRequestDto(
                null,  // Missing callId
                "2024-CALL-001",
                Priority.High,
                CallStatus.Received,
                Instant.now(),
                "Description",
                CallType.Emergency
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testReceiveCall_WithInvalidPriority_Returns400() throws Exception {
        // Given - invalid priority enum value
        String requestJson = """
                {
                    "callId": "CALL-002",
                    "callNumber": "2024-CALL-002",
                    "priority": "InvalidPriority",
                    "status": "Received",
                    "callType": "Emergency"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/calls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testDispatchCall_WithValidData_ProducesEvent() throws Exception {
        // Given
        String callId = "CALL-100";
        Instant dispatchedTime = Instant.now();
        callExistenceService.addExistingCall(callId);

        DispatchCallRequestDto request = new DispatchCallRequestDto(dispatchedTime);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/dispatch", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Call dispatch recorded"))
                .andExpect(jsonPath("$.data.callId").value(callId));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(callId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        DispatchCallRequested event = eventObjectMapper.readValue(record.value(), DispatchCallRequested.class);
        assertThat(event.getEventType()).isEqualTo("DispatchCallRequested");
        assertThat(event.getCallId()).isEqualTo(callId);
        assertThat(event.getDispatchedTime()).isEqualTo(dispatchedTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getAggregateId()).isEqualTo(callId);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void testDispatchCall_WithNonExistentCallId_Returns404() throws Exception {
        // Given
        String missingCallId = "CALL-999";
        DispatchCallRequestDto request = new DispatchCallRequestDto(Instant.now());

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/dispatch", missingCallId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testArriveAtCall_WithValidData_ProducesEvent() throws Exception {
        // Given
        String callId = "CALL-200";
        Instant arrivedTime = Instant.now();

        ArriveAtCallRequestDto request = new ArriveAtCallRequestDto(arrivedTime);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/arrive", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Call arrival recorded"))
                .andExpect(jsonPath("$.data.callId").value(callId));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(callId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        ArriveAtCallRequested event = eventObjectMapper.readValue(record.value(), ArriveAtCallRequested.class);
        assertThat(event.getEventType()).isEqualTo("ArriveAtCallRequested");
        assertThat(event.getCallId()).isEqualTo(callId);
        assertThat(event.getArrivedTime()).isEqualTo(arrivedTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getAggregateId()).isEqualTo(callId);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void testArriveAtCall_WithMissingArrivedTime_Returns400() throws Exception {
        // Given
        String callId = "CALL-201";
        ArriveAtCallRequestDto request = new ArriveAtCallRequestDto(null);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/arrive", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testClearCall_WithValidData_ProducesEvent() throws Exception {
        // Given
        String callId = "CALL-300";
        Instant clearedTime = Instant.now();

        ClearCallRequestDto request = new ClearCallRequestDto(clearedTime);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/clear", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Call cleared"))
                .andExpect(jsonPath("$.data.callId").value(callId));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(callId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        ClearCallRequested event = eventObjectMapper.readValue(record.value(), ClearCallRequested.class);
        assertThat(event.getEventType()).isEqualTo("ClearCallRequested");
        assertThat(event.getCallId()).isEqualTo(callId);
        assertThat(event.getClearedTime()).isEqualTo(clearedTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getAggregateId()).isEqualTo(callId);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void testClearCall_WithMissingClearedTime_Returns400() throws Exception {
        // Given
        String callId = "CALL-301";
        ClearCallRequestDto request = new ClearCallRequestDto(null);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/clear", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeCallStatus_WithValidStatus_ProducesEvent() throws Exception {
        // Given
        String callId = "CALL-400";
        String status = "OnScene";
        ChangeCallStatusRequestDto request = new ChangeCallStatusRequestDto(status);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/calls/{callId}/status", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Call status updated"))
                .andExpect(jsonPath("$.data.callId").value(callId));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(callId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        ChangeCallStatusRequested event = eventObjectMapper.readValue(record.value(), ChangeCallStatusRequested.class);
        assertThat(event.getEventType()).isEqualTo("ChangeCallStatusRequested");
        assertThat(event.getCallId()).isEqualTo(callId);
        assertThat(event.getStatus()).isEqualTo(status);
        assertThat(event.getAggregateId()).isEqualTo(callId);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void testChangeCallStatus_WithMissingStatus_Returns400() throws Exception {
        // Given
        String callId = "CALL-401";
        ChangeCallStatusRequestDto request = new ChangeCallStatusRequestDto(null);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/calls/{callId}/status", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    /**
     * Test-only in-memory call existence service to control 404 scenarios.
     */
    static class InMemoryCallExistenceService implements CallExistenceService {
        private final Set<String> existingCalls = new HashSet<>();

        @Override
        public boolean exists(String callId) {
            return existingCalls.contains(callId);
        }

        void addExistingCall(String callId) {
            existingCalls.add(callId);
        }

        void clear() {
            existingCalls.clear();
        }
    }

    /**
     * Test configuration to override CallExistenceService with in-memory implementation.
     */
    @Configuration
    static class TestCallExistenceConfig {
        @Bean
        @Primary
        InMemoryCallExistenceService inMemoryCallExistenceService() {
            return new InMemoryCallExistenceService();
        }

        @Bean
        @Primary
        ObjectMapper testObjectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper;
        }
    }
}
