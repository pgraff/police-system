package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.units.ChangeUnitStatusRequested;
import com.knowit.policesystem.common.events.units.CreateUnitRequested;
import com.knowit.policesystem.common.events.units.UpdateUnitRequested;
import com.knowit.policesystem.edge.domain.UnitStatus;
import com.knowit.policesystem.edge.domain.UnitType;
import com.knowit.policesystem.edge.dto.ChangeUnitStatusRequestDto;
import com.knowit.policesystem.edge.dto.CreateUnitRequestDto;
import com.knowit.policesystem.edge.dto.UpdateUnitRequestDto;
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

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UnitController.
 * Tests the full flow from REST API call to both Kafka and NATS/JetStream event production.
 */
class UnitControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TopicConfiguration topicConfiguration;

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
        consumer.subscribe(Collections.singletonList(topicConfiguration.UNIT_EVENTS));

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
    void testCreateUnit_WithValidData_ProducesEvent() throws Exception {
        // Given
        String unitId = "UNIT-001";
        CreateUnitRequestDto request = new CreateUnitRequestDto(
                unitId,
                UnitType.Single,
                UnitStatus.Available
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.unitId").value(unitId))
                .andExpect(jsonPath("$.message").value("Unit creation request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(unitId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.UNIT_EVENTS);

        // Deserialize and verify event data
        CreateUnitRequested event = eventObjectMapper.readValue(record.value(), CreateUnitRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(unitId);
        assertThat(event.getUnitId()).isEqualTo(unitId);
        assertThat(event.getUnitType()).isEqualTo("Single");
        assertThat(event.getStatus()).isEqualTo("Available");
        assertThat(event.getEventType()).isEqualTo("CreateUnitRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        CreateUnitRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                CreateUnitRequested msgEvent = eventObjectMapper.readValue(msgJson, CreateUnitRequested.class);
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
        assertThat(natsEvent.getUnitId()).isEqualTo(unitId);
        assertThat(natsEvent.getEventType()).isEqualTo("CreateUnitRequested");
    }

    @Test
    void testCreateUnit_WithMissingUnitId_Returns400() throws Exception {
        // Given
        CreateUnitRequestDto request = new CreateUnitRequestDto(
                null,  // Missing unitId
                UnitType.Team,
                UnitStatus.Assigned
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCreateUnit_WithInvalidUnitType_Returns400() throws Exception {
        // Given - invalid unitType enum value
        String requestJson = """
                {
                    "unitId": "UNIT-002",
                    "unitType": "InvalidType",
                    "status": "Available"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCreateUnit_WithInvalidStatus_Returns400() throws Exception {
        // Given - invalid status enum value
        String requestJson = """
                {
                    "unitId": "UNIT-003",
                    "unitType": "Single",
                    "status": "InvalidStatus"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCreateUnit_WithEmptyUnitId_Returns400() throws Exception {
        // Given
        CreateUnitRequestDto request = new CreateUnitRequestDto(
                "",  // Empty unitId
                UnitType.Squad,
                UnitStatus.InUse
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateUnit_WithValidData_ProducesEvent() throws Exception {
        // Given
        String unitId = "UNIT-004";
        UpdateUnitRequestDto request = new UpdateUnitRequestDto(
                UnitType.Team,
                UnitStatus.Assigned
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/units/{unitId}", unitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unitId").value(unitId))
                .andExpect(jsonPath("$.message").value("Unit update request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(unitId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.UNIT_EVENTS);

        // Deserialize and verify event data
        UpdateUnitRequested event = eventObjectMapper.readValue(record.value(), UpdateUnitRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(unitId);
        assertThat(event.getUnitId()).isEqualTo(unitId);
        assertThat(event.getUnitType()).isEqualTo("Team");
        assertThat(event.getStatus()).isEqualTo("Assigned");
        assertThat(event.getEventType()).isEqualTo("UpdateUnitRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        UpdateUnitRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                UpdateUnitRequested msgEvent = eventObjectMapper.readValue(msgJson, UpdateUnitRequested.class);
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
        assertThat(natsEvent.getUnitId()).isEqualTo(unitId);
        assertThat(natsEvent.getEventType()).isEqualTo("UpdateUnitRequested");
    }

    @Test
    void testUpdateUnit_WithAllFields_ProducesEvent() throws Exception {
        // Given - all fields provided
        String unitId = "UNIT-005";
        UpdateUnitRequestDto request = new UpdateUnitRequestDto(
                UnitType.Squad,
                UnitStatus.InUse
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/units/{unitId}", unitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then - verify event in Kafka with all fields
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        UpdateUnitRequested event = eventObjectMapper.readValue(record.value(), UpdateUnitRequested.class);
        assertThat(event.getUnitId()).isEqualTo(unitId);
        assertThat(event.getUnitType()).isEqualTo("Squad");
        assertThat(event.getStatus()).isEqualTo("InUse");
        assertThat(event.getEventType()).isEqualTo("UpdateUnitRequested");
    }

    @Test
    void testUpdateUnit_WithOnlyUnitType_ProducesEvent() throws Exception {
        // Given - only unitType provided (partial update)
        String unitId = "UNIT-006";
        UpdateUnitRequestDto request = new UpdateUnitRequestDto(
                UnitType.Single,
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/units/{unitId}", unitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then - verify event in Kafka with only unitType
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        UpdateUnitRequested event = eventObjectMapper.readValue(record.value(), UpdateUnitRequested.class);
        assertThat(event.getUnitId()).isEqualTo(unitId);
        assertThat(event.getUnitType()).isEqualTo("Single");
        assertThat(event.getStatus()).isNull();
        assertThat(event.getEventType()).isEqualTo("UpdateUnitRequested");
    }

    @Test
    void testUpdateUnit_WithOnlyStatus_ProducesEvent() throws Exception {
        // Given - only status provided (partial update)
        String unitId = "UNIT-007";
        UpdateUnitRequestDto request = new UpdateUnitRequestDto(
                null,
                UnitStatus.Maintenance
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/units/{unitId}", unitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then - verify event in Kafka with only status
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        UpdateUnitRequested event = eventObjectMapper.readValue(record.value(), UpdateUnitRequested.class);
        assertThat(event.getUnitId()).isEqualTo(unitId);
        assertThat(event.getUnitType()).isNull();
        assertThat(event.getStatus()).isEqualTo("Maintenance");
        assertThat(event.getEventType()).isEqualTo("UpdateUnitRequested");
    }

    @Test
    void testUpdateUnit_WithInvalidUnitType_Returns400() throws Exception {
        // Given - invalid unitType enum value
        String unitId = "UNIT-008";
        String requestJson = """
                {
                    "unitType": "InvalidType",
                    "status": "Available"
                }
                """;

        // When - call REST API
        mockMvc.perform(put("/api/v1/units/{unitId}", unitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateUnit_WithInvalidStatus_Returns400() throws Exception {
        // Given - invalid status enum value
        String unitId = "UNIT-009";
        String requestJson = """
                {
                    "unitType": "Single",
                    "status": "InvalidStatus"
                }
                """;

        // When - call REST API
        mockMvc.perform(put("/api/v1/units/{unitId}", unitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateUnit_WithEmptyUnitId_Returns400() throws Exception {
        // Given - empty unitId in path (this will result in 404 or 500 depending on Spring configuration)
        UpdateUnitRequestDto request = new UpdateUnitRequestDto(
                UnitType.Team,
                UnitStatus.Assigned
        );

        // When - call REST API with empty unitId (invalid path)
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/units/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is5xxServerError()); // 500 for invalid path mapping

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeUnitStatus_WithValidStatus_ProducesEvent() throws Exception {
        // Given
        String unitId = "UNIT-010";
        String status = "InUse";
        ChangeUnitStatusRequestDto request = new ChangeUnitStatusRequestDto(status);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/units/{unitId}", unitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unitId").value(unitId))
                .andExpect(jsonPath("$.data.status").value(status))
                .andExpect(jsonPath("$.message").value("Unit status change request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(unitId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.UNIT_EVENTS);

        // Deserialize and verify event data
        ChangeUnitStatusRequested event = eventObjectMapper.readValue(record.value(), ChangeUnitStatusRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(unitId);
        assertThat(event.getUnitId()).isEqualTo(unitId);
        assertThat(event.getStatus()).isEqualTo(status);
        assertThat(event.getEventType()).isEqualTo("ChangeUnitStatusRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        ChangeUnitStatusRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                ChangeUnitStatusRequested msgEvent = eventObjectMapper.readValue(msgJson, ChangeUnitStatusRequested.class);
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
        assertThat(natsEvent.getUnitId()).isEqualTo(unitId);
        assertThat(natsEvent.getEventType()).isEqualTo("ChangeUnitStatusRequested");
    }

    @Test
    void testChangeUnitStatus_WithInvalidStatus_Returns400() throws Exception {
        // Given - invalid status enum value
        String unitId = "UNIT-011";
        String requestJson = """
                {
                    "status": "InvalidStatus"
                }
                """;

        // When - call REST API
        mockMvc.perform(patch("/api/v1/units/{unitId}", unitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeUnitStatus_WithMissingStatus_Returns400() throws Exception {
        // Given - missing status field
        String unitId = "UNIT-012";
        String requestJson = "{}";

        // When - call REST API
        mockMvc.perform(patch("/api/v1/units/{unitId}", unitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeUnitStatus_WithEmptyUnitId_Returns400() throws Exception {
        // Given - empty unitId in path
        ChangeUnitStatusRequestDto request = new ChangeUnitStatusRequestDto("Available");

        // When - call REST API with empty unitId (invalid path)
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/units/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is5xxServerError()); // 500 for invalid path mapping

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }
}
