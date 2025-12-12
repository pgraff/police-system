package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.incidents.ArriveAtIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ChangeIncidentStatusRequested;
import com.knowit.policesystem.common.events.incidents.ClearIncidentRequested;
import com.knowit.policesystem.common.events.incidents.DispatchIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.common.events.incidents.UpdateIncidentRequested;
import com.knowit.policesystem.edge.domain.IncidentStatus;
import com.knowit.policesystem.edge.domain.IncidentType;
import com.knowit.policesystem.edge.domain.Priority;
import com.knowit.policesystem.edge.dto.ArriveAtIncidentRequestDto;
import com.knowit.policesystem.edge.dto.ChangeIncidentStatusRequestDto;
import com.knowit.policesystem.edge.dto.ClearIncidentRequestDto;
import com.knowit.policesystem.edge.dto.DispatchIncidentRequestDto;
import com.knowit.policesystem.edge.dto.ReportIncidentRequestDto;
import com.knowit.policesystem.edge.dto.UpdateIncidentRequestDto;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.knowit.policesystem.edge.services.incidents.IncidentExistenceService;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for IncidentController.
 * Tests the full flow from REST API call to both Kafka and NATS/JetStream event production.
 */
@Import(IncidentControllerTest.TestIncidentServiceConfig.class)
class IncidentControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TopicConfiguration topicConfiguration;

    @Autowired
    private IncidentExistenceService incidentExistenceService;

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
        consumer.subscribe(Collections.singletonList(topicConfiguration.INCIDENT_EVENTS));

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

        // Clear in-memory services
        if (incidentExistenceService instanceof InMemoryIncidentExistenceService) {
            ((InMemoryIncidentExistenceService) incidentExistenceService).clear();
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
    void testReportIncident_WithValidData_ProducesEvent() throws Exception {
        // Given
        String incidentId = "INC-001";
        String incidentNumber = "2024-001";
        Instant reportedTime = Instant.now();
        ReportIncidentRequestDto request = new ReportIncidentRequestDto(
                incidentId,
                incidentNumber,
                Priority.High,
                IncidentStatus.Reported,
                reportedTime,
                "Traffic accident at Main St and Oak Ave",
                IncidentType.Traffic
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.incidentId").value(incidentId))
                .andExpect(jsonPath("$.data.incidentNumber").value(incidentNumber))
                .andExpect(jsonPath("$.message").value("Incident report request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(incidentId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.INCIDENT_EVENTS);

        // Deserialize and verify event data
        ReportIncidentRequested event = eventObjectMapper.readValue(record.value(), ReportIncidentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(incidentId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getIncidentNumber()).isEqualTo(incidentNumber);
        assertThat(event.getPriority()).isEqualTo("High");
        assertThat(event.getStatus()).isEqualTo("Reported");
        assertThat(event.getDescription()).isEqualTo("Traffic accident at Main St and Oak Ave");
        assertThat(event.getIncidentType()).isEqualTo("Traffic");
        assertThat(event.getEventType()).isEqualTo("ReportIncidentRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        ReportIncidentRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                ReportIncidentRequested msgEvent = eventObjectMapper.readValue(msgJson, ReportIncidentRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("ReportIncidentRequested");
    }

    @Test
    void testReportIncident_WithMissingIncidentId_Returns400() throws Exception {
        // Given
        ReportIncidentRequestDto request = new ReportIncidentRequestDto(
                null,  // Missing incidentId
                "2024-001",
                Priority.High,
                IncidentStatus.Reported,
                Instant.now(),
                "Description",
                IncidentType.Traffic
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testReportIncident_WithInvalidPriority_Returns400() throws Exception {
        // Given - invalid priority enum value
        String requestJson = """
                {
                    "incidentId": "INC-002",
                    "incidentNumber": "2024-002",
                    "priority": "InvalidPriority",
                    "status": "Reported",
                    "incidentType": "Traffic"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testReportIncident_WithInvalidStatus_Returns400() throws Exception {
        // Given - invalid status enum value
        String requestJson = """
                {
                    "incidentId": "INC-003",
                    "incidentNumber": "2024-003",
                    "priority": "High",
                    "status": "InvalidStatus",
                    "incidentType": "Traffic"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testReportIncident_WithInvalidIncidentType_Returns400() throws Exception {
        // Given - invalid incidentType enum value
        String requestJson = """
                {
                    "incidentId": "INC-004",
                    "incidentNumber": "2024-004",
                    "priority": "High",
                    "status": "Reported",
                    "incidentType": "InvalidType"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testReportIncident_WithEmptyIncidentId_Returns400() throws Exception {
        // Given
        ReportIncidentRequestDto request = new ReportIncidentRequestDto(
                "",  // Empty incidentId
                "2024-001",
                Priority.High,
                IncidentStatus.Reported,
                Instant.now(),
                "Description",
                IncidentType.Traffic
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testDispatchIncident_WithValidData_ProducesEvent() throws Exception {
        // Given
        String incidentId = "INC-100";
        // Add incident to in-memory existence service
        if (incidentExistenceService instanceof InMemoryIncidentExistenceService) {
            ((InMemoryIncidentExistenceService) incidentExistenceService).addExistingIncident(incidentId);
        }
        
        Instant dispatchedTime = Instant.now();
        DispatchIncidentRequestDto request = new DispatchIncidentRequestDto(dispatchedTime);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/dispatch", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incidentId").value(incidentId))
                .andExpect(jsonPath("$.message").value("Incident dispatch request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(incidentId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.INCIDENT_EVENTS);

        // Deserialize and verify event data
        DispatchIncidentRequested event = eventObjectMapper.readValue(record.value(), DispatchIncidentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(incidentId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getDispatchedTime()).isEqualTo(dispatchedTime.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        assertThat(event.getEventType()).isEqualTo("DispatchIncidentRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        DispatchIncidentRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                DispatchIncidentRequested msgEvent = eventObjectMapper.readValue(msgJson, DispatchIncidentRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("DispatchIncidentRequested");
    }

    @Test
    void testDispatchIncident_WithEmptyIncidentId_Returns400() throws Exception {
        // Given - whitespace path variable should fail validation
        String incidentId = " ";
        DispatchIncidentRequestDto request = new DispatchIncidentRequestDto(Instant.now());

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/dispatch", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testDispatchIncident_WithMissingDispatchedTime_Returns400() throws Exception {
        // Given
        String incidentId = "INC-101";
        DispatchIncidentRequestDto request = new DispatchIncidentRequestDto(null);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/dispatch", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testArriveAtIncident_WithValidData_ProducesEvent() throws Exception {
        // Given
        String incidentId = "INC-200";
        Instant arrivedTime = Instant.now();
        ArriveAtIncidentRequestDto request = new ArriveAtIncidentRequestDto(arrivedTime);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/arrive", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incidentId").value(incidentId))
                .andExpect(jsonPath("$.message").value("Incident arrival request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(incidentId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.INCIDENT_EVENTS);

        // Deserialize and verify event data
        ArriveAtIncidentRequested event = eventObjectMapper.readValue(record.value(), ArriveAtIncidentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(incidentId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getArrivedTime()).isEqualTo(arrivedTime.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        assertThat(event.getEventType()).isEqualTo("ArriveAtIncidentRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        ArriveAtIncidentRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                ArriveAtIncidentRequested msgEvent = eventObjectMapper.readValue(msgJson, ArriveAtIncidentRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("ArriveAtIncidentRequested");
    }

    @Test
    void testArriveAtIncident_WithEmptyIncidentId_Returns400() throws Exception {
        // Given - whitespace path variable should fail validation
        String incidentId = " ";
        ArriveAtIncidentRequestDto request = new ArriveAtIncidentRequestDto(Instant.now());

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/arrive", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testArriveAtIncident_WithMissingArrivedTime_Returns400() throws Exception {
        // Given
        String incidentId = "INC-201";
        ArriveAtIncidentRequestDto request = new ArriveAtIncidentRequestDto(null);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/arrive", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testClearIncident_WithValidData_ProducesEvent() throws Exception {
        // Given
        String incidentId = "INC-300";
        Instant clearedTime = Instant.now();
        ClearIncidentRequestDto request = new ClearIncidentRequestDto(clearedTime);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/clear", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incidentId").value(incidentId))
                .andExpect(jsonPath("$.message").value("Incident clear request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(incidentId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.INCIDENT_EVENTS);

        ClearIncidentRequested event = eventObjectMapper.readValue(record.value(), ClearIncidentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(incidentId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getClearedTime()).isEqualTo(clearedTime.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        assertThat(event.getEventType()).isEqualTo("ClearIncidentRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        ClearIncidentRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                ClearIncidentRequested msgEvent = eventObjectMapper.readValue(msgJson, ClearIncidentRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("ClearIncidentRequested");
    }

    @Test
    void testClearIncident_WithEmptyIncidentId_Returns400() throws Exception {
        // Given - whitespace path variable should fail validation
        String incidentId = " ";
        ClearIncidentRequestDto request = new ClearIncidentRequestDto(Instant.now());

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/clear", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testClearIncident_WithMissingClearedTime_Returns400() throws Exception {
        // Given
        String incidentId = "INC-301";
        ClearIncidentRequestDto request = new ClearIncidentRequestDto(null);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/clear", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeIncidentStatus_WithValidStatus_ProducesEvent() throws Exception {
        // Given
        String incidentId = "INC-400";
        ChangeIncidentStatusRequestDto request = new ChangeIncidentStatusRequestDto(IncidentStatus.InProgress);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/incidents/{incidentId}/status", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incidentId").value(incidentId))
                .andExpect(jsonPath("$.message").value("Incident status change request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(incidentId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.INCIDENT_EVENTS);

        ChangeIncidentStatusRequested event = eventObjectMapper.readValue(record.value(), ChangeIncidentStatusRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(incidentId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getStatus()).isEqualTo("InProgress");
        assertThat(event.getEventType()).isEqualTo("ChangeIncidentStatusRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        ChangeIncidentStatusRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                ChangeIncidentStatusRequested msgEvent = eventObjectMapper.readValue(msgJson, ChangeIncidentStatusRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("ChangeIncidentStatusRequested");
    }

    @Test
    void testChangeIncidentStatus_WithInvalidStatus_Returns400() throws Exception {
        // Given
        String incidentId = "INC-401";
        String requestJson = "{\"status\":\"InvalidStatus\"}";

        // When - call REST API
        mockMvc.perform(patch("/api/v1/incidents/{incidentId}/status", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeIncidentStatus_WithEmptyIncidentId_Returns400() throws Exception {
        // Given - whitespace path variable should fail validation
        String incidentId = " ";
        ChangeIncidentStatusRequestDto request = new ChangeIncidentStatusRequestDto(IncidentStatus.Reported);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/incidents/{incidentId}/status", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateIncident_WithValidData_ProducesEvent() throws Exception {
        // Given
        String incidentId = "INC-200";
        // Add incident to in-memory existence service
        if (incidentExistenceService instanceof InMemoryIncidentExistenceService) {
            ((InMemoryIncidentExistenceService) incidentExistenceService).addExistingIncident(incidentId);
        }
        
        UpdateIncidentRequestDto request = new UpdateIncidentRequestDto(
                Priority.High,
                "Updated description: Traffic accident with injuries",
                IncidentType.Traffic
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/incidents/{incidentId}", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incidentId").value(incidentId))
                .andExpect(jsonPath("$.message").value("Incident update request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(incidentId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.INCIDENT_EVENTS);

        // Deserialize and verify event data
        UpdateIncidentRequested event = eventObjectMapper.readValue(record.value(), UpdateIncidentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(incidentId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getPriority()).isEqualTo("High");
        assertThat(event.getDescription()).isEqualTo("Updated description: Traffic accident with injuries");
        assertThat(event.getIncidentType()).isEqualTo("Traffic");
        assertThat(event.getEventType()).isEqualTo("UpdateIncidentRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testUpdateIncident_WithPartialData_ProducesEvent() throws Exception {
        // Given - only description provided
        String incidentId = "INC-201";
        // Add incident to in-memory existence service
        if (incidentExistenceService instanceof InMemoryIncidentExistenceService) {
            ((InMemoryIncidentExistenceService) incidentExistenceService).addExistingIncident(incidentId);
        }
        
        UpdateIncidentRequestDto request = new UpdateIncidentRequestDto(
                null,
                "Only updating description",
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/incidents/{incidentId}", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incidentId").value(incidentId))
                .andExpect(jsonPath("$.message").value("Incident update request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(incidentId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.INCIDENT_EVENTS);

        // Deserialize and verify event data
        UpdateIncidentRequested event = eventObjectMapper.readValue(record.value(), UpdateIncidentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(incidentId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getPriority()).isNull();
        assertThat(event.getDescription()).isEqualTo("Only updating description");
        assertThat(event.getIncidentType()).isNull();
        assertThat(event.getEventType()).isEqualTo("UpdateIncidentRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testUpdateIncident_WithEmptyBody_ProducesEvent() throws Exception {
        // Given - empty body
        String incidentId = "INC-202";
        // Add incident to in-memory existence service
        if (incidentExistenceService instanceof InMemoryIncidentExistenceService) {
            ((InMemoryIncidentExistenceService) incidentExistenceService).addExistingIncident(incidentId);
        }
        
        String requestJson = "{}";

        // When - call REST API
        mockMvc.perform(put("/api/v1/incidents/{incidentId}", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incidentId").value(incidentId))
                .andExpect(jsonPath("$.message").value("Incident update request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(incidentId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.INCIDENT_EVENTS);

        // Deserialize and verify event data
        UpdateIncidentRequested event = eventObjectMapper.readValue(record.value(), UpdateIncidentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(incidentId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getPriority()).isNull();
        assertThat(event.getDescription()).isNull();
        assertThat(event.getIncidentType()).isNull();
        assertThat(event.getEventType()).isEqualTo("UpdateIncidentRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testUpdateIncident_WithInvalidPriority_Returns400() throws Exception {
        // Given - invalid priority enum value
        String incidentId = "INC-203";
        String requestJson = """
                {
                    "priority": "InvalidPriority",
                    "description": "Test description"
                }
                """;

        // When - call REST API
        mockMvc.perform(put("/api/v1/incidents/{incidentId}", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateIncident_WithInvalidIncidentType_Returns400() throws Exception {
        // Given - invalid incidentType enum value
        String incidentId = "INC-204";
        String requestJson = """
                {
                    "incidentType": "InvalidType",
                    "description": "Test description"
                }
                """;

        // When - call REST API
        mockMvc.perform(put("/api/v1/incidents/{incidentId}", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateIncident_WithEmptyIncidentId_Returns400() throws Exception {
        // Given - whitespace path variable should fail validation
        String incidentId = " ";
        UpdateIncidentRequestDto request = new UpdateIncidentRequestDto(
                Priority.High,
                "Test description",
                IncidentType.Traffic
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/incidents/{incidentId}", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateIncident_WithNonExistentIncidentId_Returns404() throws Exception {
        // Given - incident doesn't exist in projection
        String nonExistentIncidentId = "INC-999";
        UpdateIncidentRequestDto request = new UpdateIncidentRequestDto(
                Priority.High,
                "Updated description",
                IncidentType.Traffic
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/incidents/{incidentId}", nonExistentIncidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Incident not found: " + nonExistentIncidentId));

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testDispatchIncident_WithNonExistentIncidentId_Returns404() throws Exception {
        // Given - incident doesn't exist in projection
        String nonExistentIncidentId = "INC-998";
        DispatchIncidentRequestDto request = new DispatchIncidentRequestDto(Instant.now());

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/dispatch", nonExistentIncidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Incident not found: " + nonExistentIncidentId));

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    /**
     * Test-only in-memory incident existence service to control 404 scenarios.
     */
    static class InMemoryIncidentExistenceService extends IncidentExistenceService {
        private final Set<String> existingIncidents = new HashSet<>();

        public InMemoryIncidentExistenceService() {
            super(null); // Pass null since we override exists()
        }

        @Override
        public boolean exists(String incidentId) {
            return existingIncidents.contains(incidentId);
        }

        void addExistingIncident(String incidentId) {
            existingIncidents.add(incidentId);
        }

        void clear() {
            existingIncidents.clear();
        }
    }

    /**
     * Test configuration to override IncidentExistenceService with in-memory implementation.
     */
    @TestConfiguration
    static class TestIncidentServiceConfig {
        @Bean
        @Primary
        IncidentExistenceService incidentExistenceService() {
            return new InMemoryIncidentExistenceService();
        }
    }
}
