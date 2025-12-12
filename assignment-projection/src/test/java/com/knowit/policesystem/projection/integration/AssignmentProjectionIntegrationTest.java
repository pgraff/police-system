package com.knowit.policesystem.projection.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.assignments.ChangeAssignmentStatusRequested;
import com.knowit.policesystem.common.events.assignments.CompleteAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.LinkAssignmentToDispatchRequested;
import com.knowit.policesystem.common.events.resourceassignment.AssignResourceRequested;
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

class AssignmentProjectionIntegrationTest extends IntegrationTestBase {

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

        try {
            jdbcTemplate.execute("TRUNCATE assignment_status_history, assignment_resource, assignment_projection RESTART IDENTITY CASCADE");
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
    void createAssignment_shouldPersistProjectionAndExposeQuery() throws Exception {
        String assignmentId = "ASN-" + UUID.randomUUID();
        Instant assignedTime = Instant.now();
        CreateAssignmentRequested event = new CreateAssignmentRequested(
                assignmentId, assignmentId, assignedTime, "Patrol", "Created", "INC-1", null
        );

        publishToKafka(assignmentId, event);

        ResponseEntity<Map> response = awaitAssignment(assignmentId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("assignmentId")).isEqualTo(assignmentId);
        assertThat(body.get("assignmentType")).isEqualTo("Patrol");
        assertThat(body.get("status")).isEqualTo("Created");
        assertThat(body.get("incidentId")).isEqualTo("INC-1");
        assertThat(body.get("callId")).isNull();
    }

    @Test
    void changeStatus_shouldUpdateStatusAndAppendHistoryWithoutDuplicates() throws Exception {
        String assignmentId = "ASN-" + UUID.randomUUID();
        Instant assignedTime = Instant.now();
        publishToKafka(assignmentId, new CreateAssignmentRequested(
                assignmentId, assignmentId, assignedTime, "Patrol", "Created", "INC-1", null
        ));

        publishToKafka(assignmentId, new ChangeAssignmentStatusRequested(assignmentId, "InProgress"));
        publishToKafka(assignmentId, new ChangeAssignmentStatusRequested(assignmentId, "InProgress")); // duplicate
        publishToKafka(assignmentId, new ChangeAssignmentStatusRequested(assignmentId, "Completed"));

        ResponseEntity<Map> assignmentResponse = awaitAssignment(assignmentId);
        assertThat(assignmentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(assignmentResponse.getBody()).isNotNull();
        assertThat(assignmentResponse.getBody().get("status")).isEqualTo("Completed");

        ResponseEntity<List> historyResponse = awaitHistory(assignmentId);
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> history = historyResponse.getBody();
        assertThat(history).isNotNull();
        assertThat(history).hasSize(2);
        assertThat(history.get(0).get("status")).isEqualTo("InProgress");
        assertThat(history.get(1).get("status")).isEqualTo("Completed");
    }

    @Test
    void completeAssignment_shouldUpdateCompletedTime() throws Exception {
        String assignmentId = "ASN-" + UUID.randomUUID();
        Instant assignedTime = Instant.now();
        publishToKafka(assignmentId, new CreateAssignmentRequested(
                assignmentId, assignmentId, assignedTime, "Patrol", "Created", "INC-1", null
        ));

        Instant completedTime = Instant.now().plusSeconds(300);
        publishToKafka(assignmentId, new CompleteAssignmentRequested(assignmentId, completedTime));

        ResponseEntity<Map> response = awaitAssignment(assignmentId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("completedTime")).isNotNull();
    }

    @Test
    void linkDispatch_shouldUpdateDispatchId() throws Exception {
        String assignmentId = "ASN-" + UUID.randomUUID();
        Instant assignedTime = Instant.now();
        publishToKafka(assignmentId, new CreateAssignmentRequested(
                assignmentId, assignmentId, assignedTime, "Patrol", "Created", null, "CALL-1"
        ));

        publishToKafka(assignmentId, new LinkAssignmentToDispatchRequested(assignmentId, assignmentId, "DISP-1"));

        ResponseEntity<Map> response = awaitAssignment(assignmentId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("dispatchId")).isEqualTo("DISP-1");
    }

    @Test
    void assignResource_shouldUpsertResourceRows() throws Exception {
        String assignmentId = "ASN-" + UUID.randomUUID();
        Instant assignedTime = Instant.now();
        publishToKafka(assignmentId, new CreateAssignmentRequested(
                assignmentId, assignmentId, assignedTime, "Patrol", "Created", "INC-1", null
        ));

        publishToKafka(assignmentId, new AssignResourceRequested(
                assignmentId, assignmentId, "OFF-1", "OFFICER", "PRIMARY", "Assigned", assignedTime
        ));
        // update same resource status
        publishToKafka(assignmentId, new AssignResourceRequested(
                assignmentId, assignmentId, "OFF-1", "OFFICER", "PRIMARY", "OnScene", assignedTime.plusSeconds(60)
        ));

        ResponseEntity<List> resources = awaitResources(assignmentId);
        assertThat(resources.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> body = resources.getBody();
        assertThat(body).isNotNull();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).get("resourceId")).isEqualTo("OFF-1");
        assertThat(body.get(0).get("status")).isEqualTo("OnScene");
    }

    @Test
    void idempotency_duplicateEventsShouldNotCorruptData() throws Exception {
        String assignmentId = "ASN-" + UUID.randomUUID();
        Instant assignedTime = Instant.now();
        CreateAssignmentRequested event = new CreateAssignmentRequested(
                assignmentId, assignmentId, assignedTime, "Patrol", "Created", "INC-1", null
        );

        publishToKafka(assignmentId, event);
        publishToKafka(assignmentId, event);

        ResponseEntity<Map> response = awaitAssignment(assignmentId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assignment_projection WHERE assignment_id = ?", Integer.class, assignmentId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void natsEvent_shouldNotDoubleProcessWhenKafkaAlsoDelivers() throws Exception {
        String assignmentId = "ASN-" + UUID.randomUUID();
        Instant assignedTime = Instant.now();
        CreateAssignmentRequested event = new CreateAssignmentRequested(
                assignmentId, assignmentId, assignedTime, "Patrol", "Created", "INC-1", null
        );

        publishToKafka(assignmentId, event);

        String subject = EventClassification.generateNatsSubject(event);
        try (Connection connection = Nats.connect(nats.getNatsUrl())) {
            connection.publish(subject, objectMapper.writeValueAsBytes(event));
        }

        ResponseEntity<Map> response = awaitAssignment(assignmentId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("assignmentId")).isEqualTo(assignmentId);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assignment_projection WHERE assignment_id = ?", Integer.class, assignmentId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void listAssignments_shouldFilterAndPaginate() throws Exception {
        String id1 = "ASN-" + UUID.randomUUID();
        String id2 = "ASN-" + UUID.randomUUID();
        Instant assignedTime = Instant.now();

        publishToKafka(id1, new CreateAssignmentRequested(
                id1, id1, assignedTime, "Patrol", "Created", "INC-1", null
        ));
        publishToKafka(id2, new CreateAssignmentRequested(
                id2, id2, assignedTime, "Rescue", "Created", null, "CALL-1"
        ));

        // Filter by incidentId
        ResponseEntity<Map> listByIncident = restTemplate.getForEntity(
                "/api/projections/assignments?incidentId={incidentId}&page=0&size=10",
                Map.class,
                "INC-1");
        assertThat(listByIncident.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body1 = listByIncident.getBody();
        assertThat(body1).isNotNull();
        assertThat(body1.get("total")).isEqualTo(1);

        // Filter by callId
        ResponseEntity<Map> listByCall = restTemplate.getForEntity(
                "/api/projections/assignments?callId={callId}&page=0&size=10",
                Map.class,
                "CALL-1");
        assertThat(listByCall.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body2 = listByCall.getBody();
        assertThat(body2).isNotNull();
        assertThat(body2.get("total")).isEqualTo(1);
    }

    private void publishToKafka(String key, Object event) throws Exception {
        String payload = objectMapper.writeValueAsString(event);
        producer.send(new ProducerRecord<>("assignment-events", key, payload)).get(10, TimeUnit.SECONDS);
    }

    private ResponseEntity<Map> awaitAssignment(String assignmentId) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> restTemplate.getForEntity("/api/projections/assignments/{id}", Map.class, assignmentId),
                        response -> response.getStatusCode().is2xxSuccessful());
    }

    private ResponseEntity<List> awaitHistory(String assignmentId) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> restTemplate.getForEntity("/api/projections/assignments/{id}/history", List.class, assignmentId),
                        response -> response.getStatusCode().is2xxSuccessful());
    }

    private ResponseEntity<List> awaitResources(String assignmentId) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> restTemplate.getForEntity("/api/projections/assignments/{id}/resources", List.class, assignmentId),
                        response -> response.getStatusCode().is2xxSuccessful());
    }
}
