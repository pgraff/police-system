package com.knowit.policesystem.projection.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.calls.ArriveAtCallRequested;
import com.knowit.policesystem.common.events.calls.ChangeCallStatusRequested;
import com.knowit.policesystem.common.events.calls.ClearCallRequested;
import com.knowit.policesystem.common.events.calls.DispatchCallRequested;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.calls.UpdateCallRequested;
import io.nats.client.Connection;
import io.nats.client.Nats;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CallProjectionIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper;
    private Properties producerProps;
    private Producer<String, String> producer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(producerProps);

        // Clean tables between tests (schema will be created by SQL init)
        try {
            jdbcTemplate.execute("TRUNCATE call_status_history, call_projection RESTART IDENTITY CASCADE");
        } catch (Exception ignored) {
            // Tables may not exist before implementation
        }
    }

    @AfterEach
    void tearDown() {
        if (producer != null) {
            producer.close();
        }
    }

    @Test
    void receiveCall_shouldPersistProjectionAndExposeQuery() throws Exception {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        ReceiveCallRequested event = new ReceiveCallRequested(
                callId, "2024-001", "High", "Received",
                receivedTime, "Traffic accident call", "Traffic"
        );

        publishToKafka(callId, event);

        ResponseEntity<Map> response = awaitCall(callId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("callId")).isEqualTo(callId);
        assertThat(body.get("callNumber")).isEqualTo("2024-001");
        assertThat(body.get("priority")).isEqualTo("High");
        assertThat(body.get("status")).isEqualTo("Received");
        assertThat(body.get("description")).isEqualTo("Traffic accident call");
        assertThat(body.get("callType")).isEqualTo("Traffic");
    }

    @Test
    void updateCall_shouldApplyPartialUpdatesAndRetainExistingFields() throws Exception {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        publishToKafka(callId, new ReceiveCallRequested(
                callId, "2024-002", "Medium", "Received",
                receivedTime, "Initial description", "Traffic"
        ));

        UpdateCallRequested update = new UpdateCallRequested(
                callId, "High", "Updated description", "Emergency"
        );
        publishToKafka(callId, update);

        ResponseEntity<Map> response = awaitCall(callId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("callId")).isEqualTo(callId);
        assertThat(body.get("callNumber")).isEqualTo("2024-002"); // unchanged
        assertThat(body.get("priority")).isEqualTo("High"); // updated
        assertThat(body.get("status")).isEqualTo("Received"); // unchanged
        assertThat(body.get("description")).isEqualTo("Updated description"); // updated
        assertThat(body.get("callType")).isEqualTo("Emergency"); // updated
    }

    @Test
    void changeStatus_shouldUpdateStatusAndAppendHistoryWithoutDuplicates() throws Exception {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        publishToKafka(callId, new ReceiveCallRequested(
                callId, "2024-003", "High", "Received",
                receivedTime, "Test call", "Traffic"
        ));

        publishToKafka(callId, new ChangeCallStatusRequested(callId, "Dispatched"));
        publishToKafka(callId, new ChangeCallStatusRequested(callId, "Dispatched")); // duplicate
        publishToKafka(callId, new ChangeCallStatusRequested(callId, "InProgress"));

        ResponseEntity<Map> callResponse = awaitCall(callId);
        assertThat(callResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(callResponse.getBody()).isNotNull();
        assertThat(callResponse.getBody().get("status")).isEqualTo("InProgress");

        ResponseEntity<List> historyResponse = awaitHistory(callId);
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> history = historyResponse.getBody();
        assertThat(history).isNotNull();
        assertThat(history).hasSize(2);
        assertThat(history.get(0).get("status")).isEqualTo("Dispatched");
        assertThat(history.get(1).get("status")).isEqualTo("InProgress");
    }

    @Test
    void dispatchCall_shouldUpdateDispatchTime() throws Exception {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        publishToKafka(callId, new ReceiveCallRequested(
                callId, "2024-004", "High", "Received",
                receivedTime, "Test call", "Traffic"
        ));

        Instant dispatchedTime = Instant.now().plusSeconds(60);
        publishToKafka(callId, new DispatchCallRequested(callId, dispatchedTime));

        ResponseEntity<Map> response = awaitCall(callId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("dispatchedTime")).isNotNull();
    }

    @Test
    void arriveAtCall_shouldUpdateArrivedTime() throws Exception {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        publishToKafka(callId, new ReceiveCallRequested(
                callId, "2024-005", "High", "Received",
                receivedTime, "Test call", "Traffic"
        ));

        Instant arrivedTime = Instant.now().plusSeconds(120);
        publishToKafka(callId, new ArriveAtCallRequested(callId, arrivedTime));

        ResponseEntity<Map> response = awaitCall(callId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("arrivedTime")).isNotNull();
    }

    @Test
    void clearCall_shouldUpdateClearedTime() throws Exception {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        publishToKafka(callId, new ReceiveCallRequested(
                callId, "2024-006", "High", "Received",
                receivedTime, "Test call", "Traffic"
        ));

        Instant clearedTime = Instant.now().plusSeconds(300);
        publishToKafka(callId, new ClearCallRequested(callId, clearedTime));

        ResponseEntity<Map> response = awaitCall(callId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("clearedTime")).isNotNull();
    }

    @Test
    void idempotency_duplicateEventsShouldNotCorruptData() throws Exception {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        ReceiveCallRequested event = new ReceiveCallRequested(
                callId, "2024-007", "High", "Received",
                receivedTime, "Test call", "Traffic"
        );

        // Publish same event twice
        publishToKafka(callId, event);
        publishToKafka(callId, event);

        ResponseEntity<Map> response = awaitCall(callId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Ensure only one projection row exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM call_projection WHERE call_id = ?", Integer.class, callId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void natsEvent_shouldNotDoubleProcessWhenKafkaAlsoDelivers() throws Exception {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        ReceiveCallRequested event = new ReceiveCallRequested(
                callId, "2024-008", "High", "Received",
                receivedTime, "Test call", "Traffic"
        );

        // Publish to Kafka
        publishToKafka(callId, event);

        // Publish same event to NATS subject
        String subject = EventClassification.generateNatsSubject(event);
        try (Connection connection = Nats.connect(nats.getNatsUrl())) {
            connection.publish(subject, objectMapper.writeValueAsBytes(event));
        }

        ResponseEntity<Map> response = awaitCall(callId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("callId")).isEqualTo(callId);

        // Ensure only one projection row exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM call_projection WHERE call_id = ?", Integer.class, callId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void listCalls_shouldFilterAndPaginate() throws Exception {
        String callId1 = "CALL-" + UUID.randomUUID();
        String callId2 = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();

        publishToKafka(callId1, new ReceiveCallRequested(
                callId1, "2024-009", "High", "Received",
                receivedTime, "High priority call", "Traffic"
        ));
        publishToKafka(callId2, new ReceiveCallRequested(
                callId2, "2024-010", "Low", "Received",
                receivedTime, "Low priority call", "Emergency"
        ));

        // Wait for both to be processed
        awaitCall(callId1);
        awaitCall(callId2);

        // Test filtering by status
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/projections/calls?status=Received&page=0&size=10", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
        assertThat(content).isNotNull();
        assertThat(content.size()).isGreaterThanOrEqualTo(2);

        // Test filtering by priority
        ResponseEntity<Map> priorityResponse = restTemplate.getForEntity(
                "/api/projections/calls?priority=High&page=0&size=10", Map.class);
        assertThat(priorityResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> priorityBody = priorityResponse.getBody();
        assertThat(priorityBody).isNotNull();
        List<Map<String, Object>> priorityContent = (List<Map<String, Object>>) priorityBody.get("content");
        assertThat(priorityContent).isNotNull();
        assertThat(priorityContent.stream()
                .anyMatch(item -> callId1.equals(item.get("callId")))).isTrue();
    }

    private void publishToKafka(String key, Object event) throws Exception {
        String payload = objectMapper.writeValueAsString(event);
        producer.send(new ProducerRecord<>("call-events", key, payload)).get(10, TimeUnit.SECONDS);
    }

    private ResponseEntity<Map> awaitCall(String callId) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> restTemplate.getForEntity("/api/projections/calls/{callId}", Map.class, callId),
                        response -> response.getStatusCode().is2xxSuccessful());
    }

    private ResponseEntity<List> awaitHistory(String callId) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> restTemplate.getForEntity("/api/projections/calls/{callId}/history", List.class, callId),
                        response -> response.getStatusCode().is2xxSuccessful());
    }
}

