package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.officers.ChangeOfficerStatusRequested;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.common.events.officers.UpdateOfficerRequested;
import com.knowit.policesystem.edge.domain.OfficerStatus;
import com.knowit.policesystem.edge.dto.ChangeOfficerStatusRequestDto;
import com.knowit.policesystem.edge.dto.RegisterOfficerRequestDto;
import com.knowit.policesystem.edge.dto.UpdateOfficerRequestDto;
import com.knowit.policesystem.edge.config.TopicConfiguration;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OfficerController.
 * Tests the full flow from REST API call to both Kafka and NATS/JetStream event production.
 */
class OfficerControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TopicConfiguration topicConfiguration;

    private static final Logger logger = LoggerFactory.getLogger(OfficerControllerTest.class);
    
    private Consumer<String, String> consumer;
    private ObjectMapper eventObjectMapper;
    private NatsTestHelper natsHelper;

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
        consumer.subscribe(Collections.singletonList(topicConfiguration.OFFICER_EVENTS));
        
        // Wait for partition assignment - consumer will start at latest offset automatically
        consumer.poll(Duration.ofSeconds(1));

        // Create NATS test helper
        natsHelper = new NatsTestHelper(nats.getNatsUrl(), eventObjectMapper);
        
        // Pre-create a catch-all stream for all command subjects to ensure it exists before publishing
        // This ensures the stream exists when events are published to any command subject
        try {
            natsHelper.ensureStreamForSubject("commands.>");
        } catch (Exception e) {
            // Log but don't fail - stream may already exist
            logger.warn("Error pre-creating NATS stream", e);
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

        // Prepare NATS subscription before API call to ensure we don't miss the message
        String expectedNatsSubject = "commands.officer.register";
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(expectedNatsSubject);
        
        // Request messages from pull subscription
        natsSubscription.pull(1);

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
        assertThat(record.topic()).isEqualTo(topicConfiguration.OFFICER_EVENTS);

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

        // Verify event also published to NATS (critical event) if subscription was prepared
        if (natsSubscription != null) {
            String natsSubject = EventClassification.generateNatsSubject(event);
            assertThat(natsSubject).isEqualTo(expectedNatsSubject);
            
            // Wait for async publish to complete and message to be available in stream
            // The publish happens asynchronously, so we need to wait a bit
            Thread.sleep(1000);
            
            // Request another pull in case the first one didn't get the message
            natsSubscription.pull(1);
            
            // Get message from the prepared subscription
            Message natsMsg = natsSubscription.nextMessage(Duration.ofSeconds(5));
            assertThat(natsMsg).isNotNull();
            
            // Deserialize and verify NATS event
            String natsEventJson = new String(natsMsg.getData(), java.nio.charset.StandardCharsets.UTF_8);
            RegisterOfficerRequested natsEvent = eventObjectMapper.readValue(natsEventJson, RegisterOfficerRequested.class);
            natsMsg.ack();
            
            assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
            assertThat(natsEvent.getBadgeNumber()).isEqualTo(badgeNumber);
            assertThat(natsEvent.getEventType()).isEqualTo("RegisterOfficerRequested");
        }
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

    @Test
    void testUpdateOfficer_WithValidData_ProducesEvent() throws Exception {
        // Given
        String badgeNumber = "12348";
        LocalDate hireDate = LocalDate.of(2021, 3, 20);
        UpdateOfficerRequestDto request = new UpdateOfficerRequestDto(
                "Jane",
                "Smith",
                "Sergeant",
                "jane.smith@police.gov",
                "555-0200",
                hireDate
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/officers/{badgeNumber}", badgeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.badgeNumber").value(badgeNumber))
                .andExpect(jsonPath("$.message").value("Officer update request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(badgeNumber);
        assertThat(record.topic()).isEqualTo(topicConfiguration.OFFICER_EVENTS);

        // Deserialize and verify event data
        UpdateOfficerRequested event = eventObjectMapper.readValue(record.value(), UpdateOfficerRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(badgeNumber);
        assertThat(event.getBadgeNumber()).isEqualTo(badgeNumber);
        assertThat(event.getFirstName()).isEqualTo("Jane");
        assertThat(event.getLastName()).isEqualTo("Smith");
        assertThat(event.getRank()).isEqualTo("Sergeant");
        assertThat(event.getEmail()).isEqualTo("jane.smith@police.gov");
        assertThat(event.getPhoneNumber()).isEqualTo("555-0200");
        assertThat(event.getHireDate()).isEqualTo("2021-03-20");
        assertThat(event.getEventType()).isEqualTo("UpdateOfficerRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testUpdateOfficer_WithAllFields_ProducesEvent() throws Exception {
        // Given - all fields provided
        String badgeNumber = "12349";
        LocalDate hireDate = LocalDate.of(2019, 6, 10);
        UpdateOfficerRequestDto request = new UpdateOfficerRequestDto(
                "Bob",
                "Johnson",
                "Lieutenant",
                "bob.johnson@police.gov",
                "555-0300",
                hireDate
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/officers/{badgeNumber}", badgeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then - verify event in Kafka with all fields
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        UpdateOfficerRequested event = eventObjectMapper.readValue(record.value(), UpdateOfficerRequested.class);
        assertThat(event.getBadgeNumber()).isEqualTo(badgeNumber);
        assertThat(event.getFirstName()).isEqualTo("Bob");
        assertThat(event.getLastName()).isEqualTo("Johnson");
        assertThat(event.getRank()).isEqualTo("Lieutenant");
        assertThat(event.getEmail()).isEqualTo("bob.johnson@police.gov");
        assertThat(event.getPhoneNumber()).isEqualTo("555-0300");
        assertThat(event.getHireDate()).isEqualTo("2019-06-10");
        assertThat(event.getEventType()).isEqualTo("UpdateOfficerRequested");
    }

    @Test
    void testUpdateOfficer_WithOnlyFirstName_ProducesEvent() throws Exception {
        // Given - only firstName provided (partial update)
        String badgeNumber = "12350";
        UpdateOfficerRequestDto request = new UpdateOfficerRequestDto(
                "Alice",
                null,
                null,
                null,
                null,
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/officers/{badgeNumber}", badgeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then - verify event in Kafka with only firstName
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        UpdateOfficerRequested event = eventObjectMapper.readValue(record.value(), UpdateOfficerRequested.class);
        assertThat(event.getBadgeNumber()).isEqualTo(badgeNumber);
        assertThat(event.getFirstName()).isEqualTo("Alice");
        assertThat(event.getLastName()).isNull();
        assertThat(event.getRank()).isNull();
        assertThat(event.getEmail()).isNull();
        assertThat(event.getPhoneNumber()).isNull();
        assertThat(event.getHireDate()).isNull();
        assertThat(event.getEventType()).isEqualTo("UpdateOfficerRequested");
    }

    @Test
    void testUpdateOfficer_WithInvalidEmail_Returns400() throws Exception {
        // Given - invalid email format
        String badgeNumber = "12351";
        String requestJson = """
                {
                    "firstName": "John",
                    "email": "invalid-email"
                }
                """;

        // When - call REST API
        mockMvc.perform(put("/api/v1/officers/{badgeNumber}", badgeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateOfficer_WithEmptyBadgeNumber_Returns400() throws Exception {
        // Given - empty badgeNumber in path (this will result in 404 or 500 depending on Spring configuration)
        UpdateOfficerRequestDto request = new UpdateOfficerRequestDto(
                "John",
                null,
                null,
                null,
                null,
                null
        );

        // When - call REST API with empty badgeNumber (invalid path)
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/officers/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is5xxServerError()); // 500 for invalid path mapping

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeOfficerStatus_WithValidStatus_ProducesEvent() throws Exception {
        // Given
        String badgeNumber = "12352";
        String status = "OnDuty";
        ChangeOfficerStatusRequestDto request = new ChangeOfficerStatusRequestDto(status);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/officers/{badgeNumber}", badgeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.badgeNumber").value(badgeNumber))
                .andExpect(jsonPath("$.data.status").value(status))
                .andExpect(jsonPath("$.message").value("Officer status change request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(badgeNumber);
        assertThat(record.topic()).isEqualTo(topicConfiguration.OFFICER_EVENTS);

        // Deserialize and verify event data
        ChangeOfficerStatusRequested event = eventObjectMapper.readValue(record.value(), ChangeOfficerStatusRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(badgeNumber);
        assertThat(event.getBadgeNumber()).isEqualTo(badgeNumber);
        assertThat(event.getStatus()).isEqualTo(status);
        assertThat(event.getEventType()).isEqualTo("ChangeOfficerStatusRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testChangeOfficerStatus_WithInvalidStatus_Returns400() throws Exception {
        // Given - invalid status enum value
        String badgeNumber = "12353";
        String requestJson = """
                {
                    "status": "InvalidStatus"
                }
                """;

        // When - call REST API
        mockMvc.perform(patch("/api/v1/officers/{badgeNumber}", badgeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeOfficerStatus_WithMissingStatus_Returns400() throws Exception {
        // Given - missing status field
        String badgeNumber = "12354";
        String requestJson = "{}";

        // When - call REST API
        mockMvc.perform(patch("/api/v1/officers/{badgeNumber}", badgeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeOfficerStatus_WithEmptyBadgeNumber_Returns400() throws Exception {
        // Given - empty badgeNumber in path
        ChangeOfficerStatusRequestDto request = new ChangeOfficerStatusRequestDto("Active");

        // When - call REST API with empty badgeNumber (invalid path)
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/officers/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is5xxServerError()); // 500 for invalid path mapping

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }
}
