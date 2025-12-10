package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.involvedparty.EndPartyInvolvementRequested;
import com.knowit.policesystem.common.events.involvedparty.InvolvePartyRequested;
import com.knowit.policesystem.edge.domain.PartyRoleType;
import com.knowit.policesystem.edge.dto.EndPartyInvolvementRequestDto;
import com.knowit.policesystem.edge.dto.InvolvePartyRequestDto;
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
 * Integration tests for InvolvedPartyController.
 * Tests the full flow from REST API call to Kafka event production.
 */
@SpringBootTest(classes = com.knowit.policesystem.edge.EdgeApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class InvolvedPartyControllerTest {

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
    private static final String TOPIC = "involved-party-events";

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
    void testInvolveParty_WithIncident_ProducesEvent() throws Exception {
        // Given
        String personId = "PERSON-001";
        String incidentId = "INC-001";
        Instant involvementStartTime = Instant.now();
        InvolvePartyRequestDto request = new InvolvePartyRequestDto(
                personId,
                incidentId,
                null,
                null,
                PartyRoleType.Witness,
                "Primary witness to the incident",
                involvementStartTime
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        String responseContent = mockMvc.perform(post("/api/v1/involved-parties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.involvementId").exists())
                .andExpect(jsonPath("$.message").value("Party involvement request processed"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract involvementId from response
        String involvementId = objectMapper.readTree(responseContent)
                .get("data")
                .get("involvementId")
                .asText();

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(involvementId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        InvolvePartyRequested event = eventObjectMapper.readValue(record.value(), InvolvePartyRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(involvementId);
        assertThat(event.getInvolvementId()).isEqualTo(involvementId);
        assertThat(event.getPersonId()).isEqualTo(personId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getCallId()).isNull();
        assertThat(event.getActivityId()).isNull();
        assertThat(event.getPartyRoleType()).isEqualTo("Witness");
        assertThat(event.getDescription()).isEqualTo("Primary witness to the incident");
        assertThat(event.getInvolvementStartTime()).isEqualTo(involvementStartTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getEventType()).isEqualTo("InvolvePartyRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testInvolveParty_WithCall_ProducesEvent() throws Exception {
        // Given
        String personId = "PERSON-002";
        String callId = "CALL-001";
        Instant involvementStartTime = Instant.now();
        InvolvePartyRequestDto request = new InvolvePartyRequestDto(
                personId,
                null,
                callId,
                null,
                PartyRoleType.Complainant,
                "Complainant in the call",
                involvementStartTime
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        String responseContent = mockMvc.perform(post("/api/v1/involved-parties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.involvementId").exists())
                .andExpect(jsonPath("$.message").value("Party involvement request processed"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract involvementId from response
        String involvementId = objectMapper.readTree(responseContent)
                .get("data")
                .get("involvementId")
                .asText();

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(involvementId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        InvolvePartyRequested event = eventObjectMapper.readValue(record.value(), InvolvePartyRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(involvementId);
        assertThat(event.getInvolvementId()).isEqualTo(involvementId);
        assertThat(event.getPersonId()).isEqualTo(personId);
        assertThat(event.getIncidentId()).isNull();
        assertThat(event.getCallId()).isEqualTo(callId);
        assertThat(event.getActivityId()).isNull();
        assertThat(event.getPartyRoleType()).isEqualTo("Complainant");
        assertThat(event.getDescription()).isEqualTo("Complainant in the call");
        assertThat(event.getInvolvementStartTime()).isEqualTo(involvementStartTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getEventType()).isEqualTo("InvolvePartyRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testInvolveParty_WithActivity_ProducesEvent() throws Exception {
        // Given
        String personId = "PERSON-003";
        String activityId = "ACT-001";
        Instant involvementStartTime = Instant.now();
        InvolvePartyRequestDto request = new InvolvePartyRequestDto(
                personId,
                null,
                null,
                activityId,
                PartyRoleType.Suspect,
                "Suspect in the activity",
                involvementStartTime
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        String responseContent = mockMvc.perform(post("/api/v1/involved-parties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.involvementId").exists())
                .andExpect(jsonPath("$.message").value("Party involvement request processed"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract involvementId from response
        String involvementId = objectMapper.readTree(responseContent)
                .get("data")
                .get("involvementId")
                .asText();

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(involvementId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        InvolvePartyRequested event = eventObjectMapper.readValue(record.value(), InvolvePartyRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(involvementId);
        assertThat(event.getInvolvementId()).isEqualTo(involvementId);
        assertThat(event.getPersonId()).isEqualTo(personId);
        assertThat(event.getIncidentId()).isNull();
        assertThat(event.getCallId()).isNull();
        assertThat(event.getActivityId()).isEqualTo(activityId);
        assertThat(event.getPartyRoleType()).isEqualTo("Suspect");
        assertThat(event.getDescription()).isEqualTo("Suspect in the activity");
        assertThat(event.getInvolvementStartTime()).isEqualTo(involvementStartTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getEventType()).isEqualTo("InvolvePartyRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testInvolveParty_WithMultipleTargets_Returns400() throws Exception {
        // Given - both incidentId and callId provided (should fail)
        String requestJson = """
                {
                    "personId": "PERSON-004",
                    "incidentId": "INC-001",
                    "callId": "CALL-001",
                    "partyRoleType": "Witness"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/involved-parties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event produced
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testInvolveParty_WithNoTargets_Returns400() throws Exception {
        // Given - no incidentId, callId, or activityId provided (should fail)
        String requestJson = """
                {
                    "personId": "PERSON-005",
                    "partyRoleType": "Witness"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/involved-parties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event produced
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testInvolveParty_WithMissingPersonId_Returns400() throws Exception {
        // Given - missing personId (should fail)
        String requestJson = """
                {
                    "incidentId": "INC-001",
                    "partyRoleType": "Witness"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/involved-parties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event produced
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testInvolveParty_WithMissingPartyRoleType_Returns400() throws Exception {
        // Given - missing partyRoleType (should fail)
        String requestJson = """
                {
                    "personId": "PERSON-006",
                    "incidentId": "INC-001"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/involved-parties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event produced
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testEndPartyInvolvement_WithValidData_ProducesEvent() throws Exception {
        // Given
        String involvementId = "INVOLVEMENT-001";
        Instant involvementEndTime = Instant.now();

        EndPartyInvolvementRequestDto request = new EndPartyInvolvementRequestDto(involvementEndTime);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        String responseContent = mockMvc.perform(post("/api/v1/involved-parties/{involvementId}/end", involvementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.involvementId").value(involvementId))
                .andExpect(jsonPath("$.message").value("Party involvement end request processed"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(involvementId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        EndPartyInvolvementRequested event = eventObjectMapper.readValue(record.value(), EndPartyInvolvementRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(involvementId);
        assertThat(event.getInvolvementId()).isEqualTo(involvementId);
        assertThat(event.getInvolvementEndTime()).isEqualTo(involvementEndTime.truncatedTo(ChronoUnit.MILLIS));
        assertThat(event.getEventType()).isEqualTo("EndPartyInvolvementRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testEndPartyInvolvement_WithMissingEndTime_Returns400() throws Exception {
        // Given - missing involvementEndTime (should fail)
        String involvementId = "INVOLVEMENT-002";
        String requestJson = "{}";

        // When - call REST API
        mockMvc.perform(post("/api/v1/involved-parties/{involvementId}/end", involvementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event produced
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }
}
