package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.dispatches.ChangeDispatchStatusRequested;
import com.knowit.policesystem.common.events.dispatches.CreateDispatchRequested;
import com.knowit.policesystem.edge.domain.DispatchStatus;
import com.knowit.policesystem.edge.domain.DispatchType;
import com.knowit.policesystem.edge.dto.ChangeDispatchStatusRequestDto;
import com.knowit.policesystem.edge.dto.CreateDispatchRequestDto;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import com.knowit.policesystem.edge.services.dispatches.DispatchExistenceService;
import java.util.HashSet;
import java.util.Set;

/**
 * Integration tests for DispatchController.
 * Tests the full flow from REST API call to both Kafka and NATS/JetStream event production.
 */
@org.springframework.test.context.junit.jupiter.SpringJUnitConfig(DispatchControllerTest.TestDispatchServiceConfig.class)
class DispatchControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DispatchController dispatchController;

    @Autowired
    private InMemoryDispatchExistenceService dispatchExistenceService;

    @Autowired
    private TopicConfiguration topicConfiguration;

    private Consumer<String, String> consumer;
    private ObjectMapper eventObjectMapper;
    private NatsTestHelper natsHelper;

    @BeforeEach
    void setUp() throws Exception {
        // Clear in-memory existence service
        dispatchExistenceService.clear();
        
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
        consumer.subscribe(Collections.singletonList(topicConfiguration.DISPATCH_EVENTS));

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
        assertThat(record.topic()).isEqualTo(topicConfiguration.DISPATCH_EVENTS);

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

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        CreateDispatchRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                CreateDispatchRequested msgEvent = eventObjectMapper.readValue(msgJson, CreateDispatchRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("CreateDispatchRequested");
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

    @Test
    void testChangeDispatchStatus_WithValidStatus_ProducesEvent() throws Exception {
        // Given
        String dispatchId = "DISP-013";
        dispatchExistenceService.addExistingDispatch(dispatchId);
        ChangeDispatchStatusRequestDto request = new ChangeDispatchStatusRequestDto(DispatchStatus.Sent);

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/dispatches/{dispatchId}/status", dispatchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dispatchId").value(dispatchId))
                .andExpect(jsonPath("$.message").value("Dispatch status change request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(dispatchId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.DISPATCH_EVENTS);

        // Deserialize and verify event data
        ChangeDispatchStatusRequested event = eventObjectMapper.readValue(record.value(), ChangeDispatchStatusRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(dispatchId);
        assertThat(event.getDispatchId()).isEqualTo(dispatchId);
        assertThat(event.getStatus()).isEqualTo("Sent");
        assertThat(event.getEventType()).isEqualTo("ChangeDispatchStatusRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        Message natsMsg = null;
        ChangeDispatchStatusRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                ChangeDispatchStatusRequested msgEvent = eventObjectMapper.readValue(msgJson, ChangeDispatchStatusRequested.class);
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
        assertThat(natsEvent.getEventType()).isEqualTo("ChangeDispatchStatusRequested");

        // Test other status values
        testStatusConversion(dispatchId + "-1", DispatchStatus.Created, "Created");
        testStatusConversion(dispatchId + "-2", DispatchStatus.Acknowledged, "Acknowledged");
        testStatusConversion(dispatchId + "-3", DispatchStatus.Completed, "Completed");
        testStatusConversion(dispatchId + "-4", DispatchStatus.Cancelled, "Cancelled");
    }

    private void testStatusConversion(String dispatchId, DispatchStatus status, String expectedStatusString) throws Exception {
        dispatchExistenceService.addExistingDispatch(dispatchId);
        ChangeDispatchStatusRequestDto request = new ChangeDispatchStatusRequestDto(status);
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/dispatches/{dispatchId}/status", dispatchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        ConsumerRecord<String, String> record = records.iterator().next();
        ChangeDispatchStatusRequested event = eventObjectMapper.readValue(record.value(), ChangeDispatchStatusRequested.class);
        assertThat(event.getStatus()).isEqualTo(expectedStatusString);
    }

    @Test
    void testChangeDispatchStatus_WithMissingStatus_Returns400() throws Exception {
        // Given - missing status in request body
        String requestJson = """
                {
                }
                """;

        // When - call REST API
        mockMvc.perform(patch("/api/v1/dispatches/{dispatchId}/status", "DISP-014")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testChangeDispatchStatus_WithInvalidStatusEnum_Returns400() throws Exception {
        // Given - invalid status enum value
        String requestJson = """
                {
                    "status": "InvalidStatus"
                }
                """;

        // When - call REST API
        mockMvc.perform(patch("/api/v1/dispatches/{dispatchId}/status", "DISP-015")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    /**
     * Test-only in-memory dispatch existence service to control 404 scenarios.
     */
    static class InMemoryDispatchExistenceService extends DispatchExistenceService {
        private final Set<String> existingDispatches = new HashSet<>();

        public InMemoryDispatchExistenceService() {
            super(null); // Pass null since we override exists()
        }

        @Override
        public boolean exists(String dispatchId) {
            return existingDispatches.contains(dispatchId);
        }

        void addExistingDispatch(String dispatchId) {
            existingDispatches.add(dispatchId);
        }

        void clear() {
            existingDispatches.clear();
        }
    }

    /**
     * Test configuration to override DispatchExistenceService with in-memory implementation.
     */
    @TestConfiguration
    static class TestDispatchServiceConfig {
        @Bean
        @Primary
        DispatchExistenceService dispatchExistenceService() {
            return new InMemoryDispatchExistenceService();
        }
    }
}
