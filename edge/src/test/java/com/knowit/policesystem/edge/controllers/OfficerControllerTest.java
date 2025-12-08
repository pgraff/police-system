package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.edge.domain.OfficerStatus;
import com.knowit.policesystem.edge.dto.RegisterOfficerRequestDto;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OfficerController.
 * Tests the full flow from REST API call to Kafka event production.
 * Note: NATS/JetStream verification is not included as NATS is disabled in test profile.
 * The DualEventPublisher will still attempt to publish to NATS, but it will be disabled.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class OfficerControllerTest {

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
    private static final String TOPIC = "officer-events";

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
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void testRegisterOfficer_WithValidData_ProducesEvent() throws Exception {
        // Given
        String badgeNumber = "12345";
        LocalDate hireDate = LocalDate.of(2020, 1, 15);
        RegisterOfficerRequestDto request = new RegisterOfficerRequestDto(
                badgeNumber,
                "John",
                "Doe",
                "Officer",
                "john.doe@police.gov",
                "555-0100",
                hireDate,
                OfficerStatus.Active
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/officers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.officerId").value(badgeNumber))
                .andExpect(jsonPath("$.data.badgeNumber").value(badgeNumber))
                .andExpect(jsonPath("$.message").value("Officer registration request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(badgeNumber);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        RegisterOfficerRequested event = eventObjectMapper.readValue(record.value(), RegisterOfficerRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(badgeNumber);
        assertThat(event.getBadgeNumber()).isEqualTo(badgeNumber);
        assertThat(event.getFirstName()).isEqualTo("John");
        assertThat(event.getLastName()).isEqualTo("Doe");
        assertThat(event.getRank()).isEqualTo("Officer");
        assertThat(event.getEmail()).isEqualTo("john.doe@police.gov");
        assertThat(event.getPhoneNumber()).isEqualTo("555-0100");
        assertThat(event.getHireDate()).isEqualTo("2020-01-15");
        assertThat(event.getStatus()).isEqualTo("Active");
        assertThat(event.getEventType()).isEqualTo("RegisterOfficerRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testRegisterOfficer_WithMissingBadgeNumber_Returns400() throws Exception {
        // Given
        RegisterOfficerRequestDto request = new RegisterOfficerRequestDto(
                null,  // Missing badgeNumber
                "John",
                "Doe",
                "Officer",
                "john.doe@police.gov",
                "555-0100",
                LocalDate.of(2020, 1, 15),
                OfficerStatus.Active
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/officers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterOfficer_WithInvalidEmail_Returns400() throws Exception {
        // Given - invalid email format
        String requestJson = """
                {
                    "badgeNumber": "12346",
                    "firstName": "John",
                    "lastName": "Doe",
                    "rank": "Officer",
                    "email": "invalid-email",
                    "status": "Active"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/officers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterOfficer_WithInvalidStatus_Returns400() throws Exception {
        // Given - invalid status enum value
        String requestJson = """
                {
                    "badgeNumber": "12347",
                    "firstName": "John",
                    "lastName": "Doe",
                    "rank": "Officer",
                    "email": "john.doe@police.gov",
                    "status": "InvalidStatus"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/officers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterOfficer_WithEmptyBadgeNumber_Returns400() throws Exception {
        // Given
        RegisterOfficerRequestDto request = new RegisterOfficerRequestDto(
                "",  // Empty badgeNumber
                "John",
                "Doe",
                "Officer",
                "john.doe@police.gov",
                "555-0100",
                LocalDate.of(2020, 1, 15),
                OfficerStatus.Active
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/officers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }
}
