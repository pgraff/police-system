package com.knowit.policesystem.projection.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.incidents.ArriveAtIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ChangeIncidentStatusRequested;
import com.knowit.policesystem.common.events.incidents.ClearIncidentRequested;
import com.knowit.policesystem.common.events.incidents.DispatchIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.common.events.incidents.UpdateIncidentRequested;
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

class IncidentProjectionIntegrationTest extends IntegrationTestBase {

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
            jdbcTemplate.execute("TRUNCATE incident_status_history, incident_projection RESTART IDENTITY CASCADE");
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
    void reportIncident_shouldPersistProjectionAndExposeQuery() throws Exception {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        ReportIncidentRequested event = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                reportedTime, "Traffic accident", "Traffic"
        );

        publishToKafka(event.getIncidentId(), event);

        ResponseEntity<Map> response = awaitIncident(incidentId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("incidentId")).isEqualTo(incidentId);
        assertThat(body.get("incidentNumber")).isEqualTo("2024-001");
        assertThat(body.get("priority")).isEqualTo("High");
        assertThat(body.get("status")).isEqualTo("Reported");
        assertThat(body.get("description")).isEqualTo("Traffic accident");
        assertThat(body.get("incidentType")).isEqualTo("Traffic");
    }

    @Test
    void updateIncident_shouldApplyPartialUpdatesAndRetainExistingFields() throws Exception {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        publishToKafka(incidentId, new ReportIncidentRequested(
                incidentId, "2024-002", "Medium", "Reported",
                reportedTime, "Initial description", "Traffic"
        ));

        UpdateIncidentRequested update = new UpdateIncidentRequested(
                incidentId, "High", "Updated description", "Emergency"
        );
        publishToKafka(incidentId, update);

        ResponseEntity<Map> response = awaitIncident(incidentId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("incidentId")).isEqualTo(incidentId);
        assertThat(body.get("incidentNumber")).isEqualTo("2024-002"); // unchanged
        assertThat(body.get("priority")).isEqualTo("High"); // updated
        assertThat(body.get("status")).isEqualTo("Reported"); // unchanged
        assertThat(body.get("description")).isEqualTo("Updated description"); // updated
        assertThat(body.get("incidentType")).isEqualTo("Emergency"); // updated
    }

    @Test
    void changeStatus_shouldUpdateStatusAndAppendHistoryWithoutDuplicates() throws Exception {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        publishToKafka(incidentId, new ReportIncidentRequested(
                incidentId, "2024-003", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        ));

        publishToKafka(incidentId, new ChangeIncidentStatusRequested(incidentId, "Dispatched"));
        publishToKafka(incidentId, new ChangeIncidentStatusRequested(incidentId, "Dispatched")); // duplicate
        publishToKafka(incidentId, new ChangeIncidentStatusRequested(incidentId, "InProgress"));

        ResponseEntity<Map> incidentResponse = awaitIncident(incidentId);
        assertThat(incidentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(incidentResponse.getBody()).isNotNull();
        assertThat(incidentResponse.getBody().get("status")).isEqualTo("InProgress");

        ResponseEntity<List> historyResponse = awaitHistory(incidentId);
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> history = historyResponse.getBody();
        assertThat(history).isNotNull();
        assertThat(history).hasSize(2);
        assertThat(history.get(0).get("status")).isEqualTo("Dispatched");
        assertThat(history.get(1).get("status")).isEqualTo("InProgress");
    }

    @Test
    void dispatchIncident_shouldUpdateDispatchTime() throws Exception {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        publishToKafka(incidentId, new ReportIncidentRequested(
                incidentId, "2024-004", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        ));

        Instant dispatchedTime = Instant.now().plusSeconds(60);
        publishToKafka(incidentId, new DispatchIncidentRequested(incidentId, dispatchedTime));

        ResponseEntity<Map> response = awaitIncident(incidentId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("dispatchedTime")).isNotNull();
    }

    @Test
    void arriveAtIncident_shouldUpdateArrivedTime() throws Exception {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        publishToKafka(incidentId, new ReportIncidentRequested(
                incidentId, "2024-005", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        ));

        Instant arrivedTime = Instant.now().plusSeconds(120);
        publishToKafka(incidentId, new ArriveAtIncidentRequested(incidentId, arrivedTime));

        ResponseEntity<Map> response = awaitIncident(incidentId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("arrivedTime")).isNotNull();
    }

    @Test
    void clearIncident_shouldUpdateClearedTime() throws Exception {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        publishToKafka(incidentId, new ReportIncidentRequested(
                incidentId, "2024-006", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        ));

        Instant clearedTime = Instant.now().plusSeconds(300);
        publishToKafka(incidentId, new ClearIncidentRequested(incidentId, clearedTime));

        ResponseEntity<Map> response = awaitIncident(incidentId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("clearedTime")).isNotNull();
    }

    @Test
    void idempotency_duplicateEventsShouldNotCorruptData() throws Exception {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        ReportIncidentRequested event = new ReportIncidentRequested(
                incidentId, "2024-007", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        );

        // Publish same event twice
        publishToKafka(incidentId, event);
        publishToKafka(incidentId, event);

        ResponseEntity<Map> response = awaitIncident(incidentId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Ensure only one projection row exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM incident_projection WHERE incident_id = ?", Integer.class, incidentId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void natsEvent_shouldNotDoubleProcessWhenKafkaAlsoDelivers() throws Exception {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        ReportIncidentRequested event = new ReportIncidentRequested(
                incidentId, "2024-008", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        );

        // Publish to Kafka
        publishToKafka(incidentId, event);

        // Publish same event to NATS subject
        String subject = EventClassification.generateNatsSubject(event);
        try (Connection connection = Nats.connect(nats.getNatsUrl())) {
            connection.publish(subject, objectMapper.writeValueAsBytes(event));
        }

        ResponseEntity<Map> response = awaitIncident(incidentId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("incidentId")).isEqualTo(incidentId);

        // Ensure only one projection row exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM incident_projection WHERE incident_id = ?", Integer.class, incidentId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void listIncidents_shouldFilterAndPaginate() throws Exception {
        String incidentId1 = "INC-" + UUID.randomUUID();
        String incidentId2 = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();

        publishToKafka(incidentId1, new ReportIncidentRequested(
                incidentId1, "2024-009", "High", "Reported",
                reportedTime, "High priority incident", "Traffic"
        ));
        publishToKafka(incidentId2, new ReportIncidentRequested(
                incidentId2, "2024-010", "Low", "Reported",
                reportedTime, "Low priority incident", "Emergency"
        ));

        // Wait for both to be processed
        awaitIncident(incidentId1);
        awaitIncident(incidentId2);

        // Test filtering by status
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/projections/incidents?status=Reported&page=0&size=10", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
        assertThat(content).isNotNull();
        assertThat(content.size()).isGreaterThanOrEqualTo(2);

        // Test filtering by priority
        ResponseEntity<Map> priorityResponse = restTemplate.getForEntity(
                "/api/projections/incidents?priority=High&page=0&size=10", Map.class);
        assertThat(priorityResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> priorityBody = priorityResponse.getBody();
        assertThat(priorityBody).isNotNull();
        List<Map<String, Object>> priorityContent = (List<Map<String, Object>>) priorityBody.get("content");
        assertThat(priorityContent).isNotNull();
        assertThat(priorityContent.stream()
                .anyMatch(item -> incidentId1.equals(item.get("incidentId")))).isTrue();
    }

    private void publishToKafka(String key, Object event) throws Exception {
        String payload = objectMapper.writeValueAsString(event);
        producer.send(new ProducerRecord<>("incident-events", key, payload)).get(10, TimeUnit.SECONDS);
    }

    private ResponseEntity<Map> awaitIncident(String incidentId) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> restTemplate.getForEntity("/api/projections/incidents/{incidentId}", Map.class, incidentId),
                        response -> response.getStatusCode().is2xxSuccessful());
    }

    private ResponseEntity<List> awaitHistory(String incidentId) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> restTemplate.getForEntity("/api/projections/incidents/{incidentId}/history", List.class, incidentId),
                        response -> response.getStatusCode().is2xxSuccessful());
    }
}
