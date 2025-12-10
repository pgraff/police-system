package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.dispatches.CreateDispatchRequested;
import com.knowit.policesystem.edge.domain.DispatchStatus;
import com.knowit.policesystem.edge.domain.DispatchType;
import com.knowit.policesystem.edge.dto.CreateDispatchRequestDto;
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
 * Integration tests for DispatchController.
 * Tests the full flow from REST API call to Kafka event production.
 */
@SpringBootTest(classes = com.knowit.policesystem.edge.EdgeApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class DispatchControllerTest {

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
    private DispatchController dispatchController;

    private Consumer<String, String> consumer;
    private ObjectMapper eventObjectMapper;
    private static final String TOPIC = "dispatch-events";

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
    void testCreateDispatch_WithValidData_ProducesEvent() throws Exception {
        // Given
        String dispatchId = "DISP-001";
        Instant dispatchTime = Instant.now();
        CreateDispatchRequestDto request = new CreateDispatchRequestDto(
                dispatchId,
                dispatchTime,
                DispatchType.Initial,
                DispatchStatus.Created
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/dispatches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dispatchId").value(dispatchId))
                .andExpect(jsonPath("$.message").value("Dispatch create request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(dispatchId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        CreateDispatchRequested event = eventObjectMapper.readValue(record.value(), CreateDispatchRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(dispatchId);
        assertThat(event.getDispatchId()).isEqualTo(dispatchId);
        assertThat(event.getDispatchTime()).isEqualTo(dispatchTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getDispatchType()).isEqualTo("Initial");
        assertThat(event.getStatus()).isEqualTo("Created");
        assertThat(event.getEventType()).isEqualTo("CreateDispatchRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testCreateDispatch_WithMissingDispatchId_Returns400() throws Exception {
        // Given
        CreateDispatchRequestDto request = new CreateDispatchRequestDto(
                null,  // Missing dispatchId
                Instant.now(),
                DispatchType.Initial,
                DispatchStatus.Created
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/dispatches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCreateDispatch_WithInvalidEnum_Returns400() throws Exception {
        // Given - invalid enum value
        String requestJson = """
                {
                    "dispatchId": "DISP-002",
                    "dispatchTime": "2024-01-15T10:30:00.000Z",
                    "dispatchType": "InvalidType",
                    "status": "Created"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/dispatches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }
}
