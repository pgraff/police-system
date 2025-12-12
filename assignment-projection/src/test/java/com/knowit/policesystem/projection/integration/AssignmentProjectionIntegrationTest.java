package com.knowit.policesystem.projection.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.activities.ChangeActivityStatusRequested;
import com.knowit.policesystem.common.events.activities.CompleteActivityRequested;
import com.knowit.policesystem.common.events.activities.StartActivityRequested;
import com.knowit.policesystem.common.events.activities.UpdateActivityRequested;
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

class ActivityProjectionIntegrationTest extends IntegrationTestBase {

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
            jdbcTemplate.execute("TRUNCATE activity_status_history, activity_projection RESTART IDENTITY CASCADE");
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
    void startActivity_shouldPersistProjectionAndExposeQuery() throws Exception {
        String activityId = "ACT-" + UUID.randomUUID();
        Instant activityTime = Instant.now();
        StartActivityRequested event = new StartActivityRequested(
                activityId, activityTime, "Investigation", "Test activity", "Started"
        );

        publishToKafka(activityId, event);

        ResponseEntity<Map> response = awaitActivity(activityId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("activityId")).isEqualTo(activityId);
        assertThat(body.get("activityType")).isEqualTo("Investigation");
        assertThat(body.get("status")).isEqualTo("Started");
        assertThat(body.get("description")).isEqualTo("Test activity");
        assertThat(body.get("activityTime")).isNotNull();
    }

    @Test
    void updateActivity_shouldApplyPartialUpdatesAndRetainExistingFields() throws Exception {
        String activityId = "ACT-" + UUID.randomUUID();
        Instant activityTime = Instant.now();
        publishToKafka(activityId, new StartActivityRequested(
                activityId, activityTime, "Investigation", "Initial description", "Started"
        ));

        UpdateActivityRequested update = new UpdateActivityRequested(
                activityId, "Updated description"
        );
        publishToKafka(activityId, update);

        ResponseEntity<Map> response = awaitActivity(activityId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("activityId")).isEqualTo(activityId);
        assertThat(body.get("activityType")).isEqualTo("Investigation"); // unchanged
        assertThat(body.get("status")).isEqualTo("Started"); // unchanged
        assertThat(body.get("description")).isEqualTo("Updated description"); // updated
    }

    @Test
    void changeStatus_shouldUpdateStatusAndAppendHistoryWithoutDuplicates() throws Exception {
        String activityId = "ACT-" + UUID.randomUUID();
        Instant activityTime = Instant.now();
        publishToKafka(activityId, new StartActivityRequested(
                activityId, activityTime, "Investigation", "Test activity", "Started"
        ));

        publishToKafka(activityId, new ChangeActivityStatusRequested(activityId, "InProgress"));
        publishToKafka(activityId, new ChangeActivityStatusRequested(activityId, "InProgress")); // duplicate
        publishToKafka(activityId, new ChangeActivityStatusRequested(activityId, "Completed"));

        ResponseEntity<Map> activityResponse = awaitActivity(activityId);
        assertThat(activityResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(activityResponse.getBody()).isNotNull();
        assertThat(activityResponse.getBody().get("status")).isEqualTo("Completed");

        ResponseEntity<List> historyResponse = awaitHistory(activityId);
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> history = historyResponse.getBody();
        assertThat(history).isNotNull();
        assertThat(history).hasSize(2);
        assertThat(history.get(0).get("status")).isEqualTo("InProgress");
        assertThat(history.get(1).get("status")).isEqualTo("Completed");
    }

    @Test
    void completeActivity_shouldUpdateCompletedTime() throws Exception {
        String activityId = "ACT-" + UUID.randomUUID();
        Instant activityTime = Instant.now();
        publishToKafka(activityId, new StartActivityRequested(
                activityId, activityTime, "Investigation", "Test activity", "Started"
        ));

        Instant completedTime = Instant.now().plusSeconds(300);
        publishToKafka(activityId, new CompleteActivityRequested(activityId, completedTime));

        ResponseEntity<Map> response = awaitActivity(activityId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("completedTime")).isNotNull();
    }

    @Test
    void idempotency_duplicateEventsShouldNotCorruptData() throws Exception {
        String activityId = "ACT-" + UUID.randomUUID();
        Instant activityTime = Instant.now();
        StartActivityRequested event = new StartActivityRequested(
                activityId, activityTime, "Investigation", "Test activity", "Started"
        );

        // Publish same event twice
        publishToKafka(activityId, event);
        publishToKafka(activityId, event);

        ResponseEntity<Map> response = awaitActivity(activityId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Ensure only one projection row exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM activity_projection WHERE activity_id = ?", Integer.class, activityId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void natsEvent_shouldNotDoubleProcessWhenKafkaAlsoDelivers() throws Exception {
        String activityId = "ACT-" + UUID.randomUUID();
        Instant activityTime = Instant.now();
        StartActivityRequested event = new StartActivityRequested(
                activityId, activityTime, "Investigation", "Test activity", "Started"
        );

        // Publish to Kafka
        publishToKafka(activityId, event);

        // Publish same event to NATS subject
        String subject = EventClassification.generateNatsSubject(event);
        try (Connection connection = Nats.connect(nats.getNatsUrl())) {
            connection.publish(subject, objectMapper.writeValueAsBytes(event));
        }

        ResponseEntity<Map> response = awaitActivity(activityId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("activityId")).isEqualTo(activityId);

        // Ensure only one projection row exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM activity_projection WHERE activity_id = ?", Integer.class, activityId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void listActivities_shouldFilterAndPaginate() throws Exception {
        String activityId1 = "ACT-" + UUID.randomUUID();
        String activityId2 = "ACT-" + UUID.randomUUID();
        Instant activityTime = Instant.now();

        publishToKafka(activityId1, new StartActivityRequested(
                activityId1, activityTime, "Investigation", "High priority activity", "Started"
        ));
        publishToKafka(activityId2, new StartActivityRequested(
                activityId2, activityTime, "Patrol", "Low priority activity", "Started"
        ));

        // Wait for both to be processed
        awaitActivity(activityId1);
        awaitActivity(activityId2);

        // Test filtering by status
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/projections/activities?status=Started&page=0&size=10", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
        assertThat(content).isNotNull();
        assertThat(content.size()).isGreaterThanOrEqualTo(2);

        // Test filtering by activityType
        ResponseEntity<Map> typeResponse = restTemplate.getForEntity(
                "/api/projections/activities?activityType=Investigation&page=0&size=10", Map.class);
        assertThat(typeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> typeBody = typeResponse.getBody();
        assertThat(typeBody).isNotNull();
        List<Map<String, Object>> typeContent = (List<Map<String, Object>>) typeBody.get("content");
        assertThat(typeContent).isNotNull();
        assertThat(typeContent.stream()
                .anyMatch(item -> activityId1.equals(item.get("activityId")))).isTrue();
    }

    private void publishToKafka(String key, Object event) throws Exception {
        String payload = objectMapper.writeValueAsString(event);
        producer.send(new ProducerRecord<>("activity-events", key, payload)).get(10, TimeUnit.SECONDS);
    }

    private ResponseEntity<Map> awaitActivity(String activityId) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> restTemplate.getForEntity("/api/projections/activities/{activityId}", Map.class, activityId),
                        response -> response.getStatusCode().is2xxSuccessful());
    }

    private ResponseEntity<List> awaitHistory(String activityId) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> restTemplate.getForEntity("/api/projections/activities/{activityId}/history", List.class, activityId),
                        response -> response.getStatusCode().is2xxSuccessful());
    }
}

