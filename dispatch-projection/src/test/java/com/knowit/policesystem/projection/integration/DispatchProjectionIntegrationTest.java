package com.knowit.policesystem.projection.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.dispatches.ChangeDispatchStatusRequested;
import com.knowit.policesystem.common.events.dispatches.CreateDispatchRequested;
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

class DispatchProjectionIntegrationTest extends IntegrationTestBase {

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
            jdbcTemplate.execute("TRUNCATE dispatch_status_history, dispatch_projection RESTART IDENTITY CASCADE");
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
    void createDispatch_shouldPersistProjectionAndExposeQuery() throws Exception {
        String dispatchId = "DISP-" + UUID.randomUUID();
        Instant dispatchTime = Instant.now();
        CreateDispatchRequested event = new CreateDispatchRequested(
                dispatchId, dispatchTime, "Emergency", "Created"
        );

        publishToKafka(dispatchId, event);

        ResponseEntity<Map> response = awaitDispatch(dispatchId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("dispatchId")).isEqualTo(dispatchId);
        assertThat(body.get("dispatchType")).isEqualTo("Emergency");
        assertThat(body.get("status")).isEqualTo("Created");
        assertThat(body.get("dispatchTime")).isNotNull();
    }

    @Test
    void changeStatus_shouldUpdateStatusAndAppendHistoryWithoutDuplicates() throws Exception {
        String dispatchId = "DISP-" + UUID.randomUUID();
        Instant dispatchTime = Instant.now();
        publishToKafka(dispatchId, new CreateDispatchRequested(
                dispatchId, dispatchTime, "Emergency", "Created"
        ));

        publishToKafka(dispatchId, new ChangeDispatchStatusRequested(dispatchId, "Dispatched"));
        publishToKafka(dispatchId, new ChangeDispatchStatusRequested(dispatchId, "Dispatched")); // duplicate
        publishToKafka(dispatchId, new ChangeDispatchStatusRequested(dispatchId, "InProgress"));

        ResponseEntity<Map> dispatchResponse = awaitDispatch(dispatchId);
        assertThat(dispatchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dispatchResponse.getBody()).isNotNull();
        assertThat(dispatchResponse.getBody().get("status")).isEqualTo("InProgress");

        ResponseEntity<List> historyResponse = awaitHistory(dispatchId);
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> history = historyResponse.getBody();
        assertThat(history).isNotNull();
        assertThat(history).hasSize(2);
        assertThat(history.get(0).get("status")).isEqualTo("Dispatched");
        assertThat(history.get(1).get("status")).isEqualTo("InProgress");
    }

    @Test
    void idempotency_duplicateEventsShouldNotCorruptData() throws Exception {
        String dispatchId = "DISP-" + UUID.randomUUID();
        Instant dispatchTime = Instant.now();
        CreateDispatchRequested event = new CreateDispatchRequested(
                dispatchId, dispatchTime, "Emergency", "Created"
        );

        // Publish same event twice
        publishToKafka(dispatchId, event);
        publishToKafka(dispatchId, event);

        ResponseEntity<Map> response = awaitDispatch(dispatchId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Ensure only one projection row exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dispatch_projection WHERE dispatch_id = ?", Integer.class, dispatchId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void natsEvent_shouldNotDoubleProcessWhenKafkaAlsoDelivers() throws Exception {
        String dispatchId = "DISP-" + UUID.randomUUID();
        Instant dispatchTime = Instant.now();
        CreateDispatchRequested event = new CreateDispatchRequested(
                dispatchId, dispatchTime, "Emergency", "Created"
        );

        // Publish to Kafka
        publishToKafka(dispatchId, event);

        // Publish same event to NATS subject
        String subject = EventClassification.generateNatsSubject(event);
        try (Connection connection = Nats.connect(nats.getNatsUrl())) {
            connection.publish(subject, objectMapper.writeValueAsBytes(event));
        }

        ResponseEntity<Map> response = awaitDispatch(dispatchId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("dispatchId")).isEqualTo(dispatchId);

        // Ensure only one projection row exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dispatch_projection WHERE dispatch_id = ?", Integer.class, dispatchId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void listDispatches_shouldFilterAndPaginate() throws Exception {
        String dispatchId1 = "DISP-" + UUID.randomUUID();
        String dispatchId2 = "DISP-" + UUID.randomUUID();
        Instant dispatchTime = Instant.now();

        publishToKafka(dispatchId1, new CreateDispatchRequested(
                dispatchId1, dispatchTime, "Emergency", "Created"
        ));
        publishToKafka(dispatchId2, new CreateDispatchRequested(
                dispatchId2, dispatchTime, "Routine", "Created"
        ));

        // Wait for both to be processed
        awaitDispatch(dispatchId1);
        awaitDispatch(dispatchId2);

        // Test filtering by status
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/projections/dispatches?status=Created&page=0&size=10", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
        assertThat(content).isNotNull();
        assertThat(content.size()).isGreaterThanOrEqualTo(2);

        // Test filtering by dispatchType
        ResponseEntity<Map> typeResponse = restTemplate.getForEntity(
                "/api/projections/dispatches?dispatchType=Emergency&page=0&size=10", Map.class);
        assertThat(typeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> typeBody = typeResponse.getBody();
        assertThat(typeBody).isNotNull();
        List<Map<String, Object>> typeContent = (List<Map<String, Object>>) typeBody.get("content");
        assertThat(typeContent).isNotNull();
        assertThat(typeContent.stream()
                .anyMatch(item -> dispatchId1.equals(item.get("dispatchId")))).isTrue();
    }

    private void publishToKafka(String key, Object event) throws Exception {
        String payload = objectMapper.writeValueAsString(event);
        producer.send(new ProducerRecord<>("dispatch-events", key, payload)).get(10, TimeUnit.SECONDS);
    }

    private ResponseEntity<Map> awaitDispatch(String dispatchId) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> restTemplate.getForEntity("/api/projections/dispatches/{dispatchId}", Map.class, dispatchId),
                        response -> response.getStatusCode().is2xxSuccessful());
    }

    private ResponseEntity<List> awaitHistory(String dispatchId) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> restTemplate.getForEntity("/api/projections/dispatches/{dispatchId}/history", List.class, dispatchId),
                        response -> response.getStatusCode().is2xxSuccessful());
    }
}

