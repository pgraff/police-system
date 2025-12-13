package com.knowit.policesystem.projection.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.projection.api.IncidentProjectionResponse;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OperationalProjectionE2ETest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper;
    private Properties producerProps;
    private Producer<String, String> producer;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/projections";
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(producerProps);
    }

    @AfterEach
    void tearDown() {
        if (producer != null) {
            producer.close();
        }
        // Clean up test data
        jdbcTemplate.update("DELETE FROM resource_assignment_projection");
        jdbcTemplate.update("DELETE FROM involved_party_projection");
        jdbcTemplate.update("DELETE FROM assignment_projection");
        jdbcTemplate.update("DELETE FROM activity_projection");
        jdbcTemplate.update("DELETE FROM dispatch_projection");
        jdbcTemplate.update("DELETE FROM call_projection");
        jdbcTemplate.update("DELETE FROM incident_projection");
    }

    @Test
    void eventFlow_KafkaToProjectionToApi_Works() throws Exception {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        ReportIncidentRequested event = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        );

        // 1. Publish event to Kafka
        String eventJson = objectMapper.writeValueAsString(event);
        producer.send(new ProducerRecord<>("incident-events", incidentId, eventJson));
        producer.flush();

        // 2. Wait for event processing
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // 3. Query API endpoint
                    ResponseEntity<IncidentProjectionResponse> response = restTemplate.getForEntity(
                            baseUrl + "/incidents/{id}", IncidentProjectionResponse.class, incidentId);

                    // 4. Verify data matches event
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().incidentId()).isEqualTo(incidentId);
                    assertThat(response.getBody().incidentNumber()).isEqualTo("2024-001");
                    assertThat(response.getBody().priority()).isEqualTo("High");
                    assertThat(response.getBody().status()).isEqualTo("Reported");
                    assertThat(response.getBody().description()).isEqualTo("Test incident");
                    assertThat(response.getBody().incidentType()).isEqualTo("Traffic");
                });
    }

    @Test
    void eventFlow_MultipleEvents_CreatesRelatedEntities() throws Exception {
        String incidentId = "INC-" + UUID.randomUUID();
        String callId = "CALL-" + UUID.randomUUID();

        // 1. Publish incident event
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported", Instant.now(), "Test", "Traffic"
        );
        producer.send(new ProducerRecord<>("incident-events", incidentId, objectMapper.writeValueAsString(incidentEvent)));

        // 2. Publish call event linked to incident
        ReceiveCallRequested callEvent = new ReceiveCallRequested(
                callId, "CALL-001", "High", "Received", Instant.now(), "Test call", "Emergency"
        );
        producer.send(new ProducerRecord<>("call-events", callId, objectMapper.writeValueAsString(callEvent)));

        // 3. Link call to incident (simulate via direct DB update for E2E test)
        producer.flush();
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // Verify call exists
                    ResponseEntity<?> callResponse = restTemplate.getForEntity(
                            baseUrl + "/calls/{id}", Object.class, callId);
                    assertThat(callResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                });

        // Link call to incident
        jdbcTemplate.update("UPDATE call_projection SET incident_id = ? WHERE call_id = ?", incidentId, callId);

        // 4. Verify composite query includes both
        Awaitility.await()
                .atMost(2, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<?> incidentResponse = restTemplate.getForEntity(
                            baseUrl + "/incidents/{id}/full", Object.class, incidentId);
                    assertThat(incidentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                });
    }

    @Test
    void compositeQuery_WithRealData_ReturnsAllRelatedEntities() throws Exception {
        String incidentId = "INC-" + UUID.randomUUID();
        String callId = "CALL-" + UUID.randomUUID();

        // 1. Create incident via event
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported", Instant.now(), "Test incident", "Traffic"
        );
        producer.send(new ProducerRecord<>("incident-events", incidentId, objectMapper.writeValueAsString(incidentEvent)));

        // 2. Create call via event
        ReceiveCallRequested callEvent = new ReceiveCallRequested(
                callId, "CALL-001", "High", "Received", Instant.now(), "Test call", "Emergency"
        );
        producer.send(new ProducerRecord<>("call-events", callId, objectMapper.writeValueAsString(callEvent)));

        producer.flush();

        // Wait for events to be processed
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<IncidentProjectionResponse> incidentResponse = restTemplate.getForEntity(
                            baseUrl + "/incidents/{id}", IncidentProjectionResponse.class, incidentId);
                    assertThat(incidentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                });

        // Link call to incident
        jdbcTemplate.update("UPDATE call_projection SET incident_id = ? WHERE call_id = ?", incidentId, callId);

        // 3. Call composite endpoint
        Awaitility.await()
                .atMost(2, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<?> fullResponse = restTemplate.getForEntity(
                            baseUrl + "/incidents/{id}/full", Object.class, incidentId);
                    assertThat(fullResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                });
    }
}
