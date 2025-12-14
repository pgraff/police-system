package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.calls.ArriveAtCallRequested;
import com.knowit.policesystem.common.events.calls.ClearCallRequested;
import com.knowit.policesystem.common.events.calls.ChangeCallStatusRequested;
import com.knowit.policesystem.common.events.calls.LinkCallToIncidentRequested;
import com.knowit.policesystem.common.events.calls.LinkCallToDispatchRequested;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.calls.DispatchCallRequested;
import com.knowit.policesystem.common.events.calls.UpdateCallRequested;
import com.knowit.policesystem.edge.domain.CallStatus;
import com.knowit.policesystem.edge.domain.CallType;
import com.knowit.policesystem.edge.domain.Priority;
import com.knowit.policesystem.edge.dto.ArriveAtCallRequestDto;
import com.knowit.policesystem.edge.dto.ChangeCallStatusRequestDto;
import com.knowit.policesystem.edge.dto.ClearCallRequestDto;
import com.knowit.policesystem.edge.dto.DispatchCallRequestDto;
import com.knowit.policesystem.edge.dto.LinkCallToIncidentRequestDto;
import com.knowit.policesystem.edge.dto.LinkCallToDispatchRequestDto;
import com.knowit.policesystem.edge.dto.ReceiveCallRequestDto;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.infrastructure.BaseIntegrationTest;
import com.knowit.policesystem.edge.infrastructure.NatsTestHelper;
import com.knowit.policesystem.edge.services.calls.CallExistenceService;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CallController.
 * Tests the full flow from REST API call to both Kafka and NATS/JetStream event production.
 */
@Import(CallControllerTest.TestCallExistenceConfig.class)
class CallControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryCallExistenceService callExistenceService;

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
        consumer.subscribe(Collections.singletonList(topicConfiguration.CALL_EVENTS));

        // Wait for partition assignment - consumer will start at latest offset automatically
        consumer.poll(Duration.ofSeconds(1));

        callExistenceService.clear();

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
        assertThat(record.topic()).isEqualTo(topicConfiguration.CALL_EVENTS);

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

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        ReceiveCallRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                ReceiveCallRequested msgEvent = eventObjectMapper.readValue(msgJson, ReceiveCallRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("ReceiveCallRequested");
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
        assertThat(record.topic()).isEqualTo(topicConfiguration.CALL_EVENTS);

        DispatchCallRequested event = eventObjectMapper.readValue(record.value(), DispatchCallRequested.class);
        assertThat(event.getEventType()).isEqualTo("DispatchCallRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        DispatchCallRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                DispatchCallRequested msgEvent = eventObjectMapper.readValue(msgJson, DispatchCallRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("DispatchCallRequested");
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
        assertThat(record.topic()).isEqualTo(topicConfiguration.CALL_EVENTS);

        ArriveAtCallRequested event = eventObjectMapper.readValue(record.value(), ArriveAtCallRequested.class);
        assertThat(event.getEventType()).isEqualTo("ArriveAtCallRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        ArriveAtCallRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                ArriveAtCallRequested msgEvent = eventObjectMapper.readValue(msgJson, ArriveAtCallRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("ArriveAtCallRequested");
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
        assertThat(record.topic()).isEqualTo(topicConfiguration.CALL_EVENTS);

        ClearCallRequested event = eventObjectMapper.readValue(record.value(), ClearCallRequested.class);
        assertThat(event.getEventType()).isEqualTo("ClearCallRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        ClearCallRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                ClearCallRequested msgEvent = eventObjectMapper.readValue(msgJson, ClearCallRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("ClearCallRequested");
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
        callExistenceService.addExistingCall(callId);
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
        assertThat(record.topic()).isEqualTo(topicConfiguration.CALL_EVENTS);

        ChangeCallStatusRequested event = eventObjectMapper.readValue(record.value(), ChangeCallStatusRequested.class);
        assertThat(event.getEventType()).isEqualTo("ChangeCallStatusRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        ChangeCallStatusRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                ChangeCallStatusRequested msgEvent = eventObjectMapper.readValue(msgJson, ChangeCallStatusRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("ChangeCallStatusRequested");
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

    @Test
    void testUpdateCall_WithValidData_ProducesEvent() throws Exception {
        // Given
        String callId = "CALL-500";
        callExistenceService.addExistingCall(callId);
        String description = "Updated description";

        String requestJson = """
                {
                    "priority": "High",
                    "description": "Updated description"
                }
                """;

        // When - call REST API
        mockMvc.perform(put("/api/v1/calls/{callId}", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Call update request processed"))
                .andExpect(jsonPath("$.data.callId").value(callId));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(callId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.CALL_EVENTS);

        UpdateCallRequested event = eventObjectMapper.readValue(record.value(), UpdateCallRequested.class);
        assertThat(event.getEventType()).isEqualTo("UpdateCallRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        UpdateCallRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                UpdateCallRequested msgEvent = eventObjectMapper.readValue(msgJson, UpdateCallRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("UpdateCallRequested");
        assertThat(event.getCallId()).isEqualTo(callId);
        assertThat(event.getPriority()).isEqualTo("High");
        assertThat(event.getDescription()).isEqualTo(description);
        assertThat(event.getCallType()).isNull();
        assertThat(event.getAggregateId()).isEqualTo(callId);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void testUpdateCall_WithNoBody_Returns400() throws Exception {
        // Given
        String callId = "CALL-501";

        // When - call REST API
        mockMvc.perform(put("/api/v1/calls/{callId}", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateCall_WithInvalidPriority_Returns400() throws Exception {
        // Given
        String callId = "CALL-502";
        String requestJson = """
                {
                    "priority": "InvalidPriority"
                }
                """;

        // When - call REST API
        mockMvc.perform(put("/api/v1/calls/{callId}", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkCallToIncident_WithValidData_ProducesEvent() throws Exception {
        // Given
        String callId = "CALL-600";
        String incidentId = "INC-001";
        callExistenceService.addExistingCall(callId);

        LinkCallToIncidentRequestDto request = new LinkCallToIncidentRequestDto(incidentId);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/incidents", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Call link request processed"))
                .andExpect(jsonPath("$.data.callId").value(callId))
                .andExpect(jsonPath("$.data.incidentId").value(incidentId));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(callId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.CALL_EVENTS);

        // Deserialize and verify event data
        LinkCallToIncidentRequested event = eventObjectMapper.readValue(record.value(), LinkCallToIncidentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(callId);
        assertThat(event.getCallId()).isEqualTo(callId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getEventType()).isEqualTo("LinkCallToIncidentRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        LinkCallToIncidentRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                LinkCallToIncidentRequested msgEvent = eventObjectMapper.readValue(msgJson, LinkCallToIncidentRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("LinkCallToIncidentRequested");
    }

    @Test
    void testLinkCallToIncident_WithMissingIncidentId_Returns400() throws Exception {
        // Given
        String callId = "CALL-601";
        callExistenceService.addExistingCall(callId);

        com.knowit.policesystem.edge.dto.LinkCallToIncidentRequestDto request =
                new com.knowit.policesystem.edge.dto.LinkCallToIncidentRequestDto(null);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/incidents", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkCallToIncident_WithNonExistentCallId_Returns404() throws Exception {
        // Given
        String missingCallId = "CALL-999";
        String incidentId = "INC-001";

        LinkCallToIncidentRequestDto request = new LinkCallToIncidentRequestDto(incidentId);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/incidents", missingCallId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkCallToDispatch_WithValidData_ProducesEvent() throws Exception {
        // Given
        String callId = "CALL-700";
        String dispatchId = "DISP-001";
        callExistenceService.addExistingCall(callId);

        LinkCallToDispatchRequestDto request = new LinkCallToDispatchRequestDto(dispatchId);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/dispatches", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Call link request processed"))
                .andExpect(jsonPath("$.data.callId").value(callId))
                .andExpect(jsonPath("$.data.dispatchId").value(dispatchId));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(callId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.CALL_EVENTS);

        // Deserialize and verify event data
        LinkCallToDispatchRequested event = eventObjectMapper.readValue(record.value(), LinkCallToDispatchRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(callId);
        assertThat(event.getCallId()).isEqualTo(callId);
        assertThat(event.getDispatchId()).isEqualTo(dispatchId);
        assertThat(event.getEventType()).isEqualTo("LinkCallToDispatchRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        LinkCallToDispatchRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                LinkCallToDispatchRequested msgEvent = eventObjectMapper.readValue(msgJson, LinkCallToDispatchRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("LinkCallToDispatchRequested");
    }

    @Test
    void testLinkCallToDispatch_WithMissingDispatchId_Returns400() throws Exception {
        // Given
        String callId = "CALL-701";
        callExistenceService.addExistingCall(callId);

        com.knowit.policesystem.edge.dto.LinkCallToDispatchRequestDto request =
                new com.knowit.policesystem.edge.dto.LinkCallToDispatchRequestDto(null);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/dispatches", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkCallToDispatch_WithNonExistentCallId_Returns404() throws Exception {
        // Given
        String missingCallId = "CALL-999";
        String dispatchId = "DISP-001";

        LinkCallToDispatchRequestDto request = new LinkCallToDispatchRequestDto(dispatchId);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/dispatches", missingCallId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());

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
     * Note: We use @TestConfiguration instead of @Configuration to ensure it doesn't interfere
     * with the main application context and component scanning.
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class TestCallExistenceConfig {
        @Bean
        @Primary
        CallExistenceService callExistenceService() {
            return new InMemoryCallExistenceService();
        }

        // Note: Removed testObjectMapper bean - tests use the main objectMapper from JacksonConfig
        // which is configured with FlexibleInstantDeserializer for date parsing
    }
}
