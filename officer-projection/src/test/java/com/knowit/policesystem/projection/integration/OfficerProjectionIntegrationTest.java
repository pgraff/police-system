package com.knowit.policesystem.projection.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.officers.ChangeOfficerStatusRequested;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.common.events.officers.UpdateOfficerRequested;
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OfficerProjectionIntegrationTest extends IntegrationTestBase {

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
            jdbcTemplate.execute("TRUNCATE officer_status_history, officer_projection RESTART IDENTITY CASCADE");
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
    void registerEvent_shouldPersistProjectionAndExposeQuery() throws Exception {
        String badge = "B-" + UUID.randomUUID();
        RegisterOfficerRequested event = new RegisterOfficerRequested(
                badge, "Jane", "Doe", "Sergeant",
                "jane.doe@example.com", "555-1234", "2023-01-01", "Active"
        );

        publishToKafka(event.getBadgeNumber(), event);

        ResponseEntity<Map> response = awaitOfficer(badge);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("badgeNumber")).isEqualTo(badge);
        assertThat(body.get("firstName")).isEqualTo("Jane");
        assertThat(body.get("lastName")).isEqualTo("Doe");
        assertThat(body.get("rank")).isEqualTo("Sergeant");
        assertThat(body.get("email")).isEqualTo("jane.doe@example.com");
        assertThat(body.get("phoneNumber")).isEqualTo("555-1234");
        assertThat(body.get("hireDate")).isEqualTo("2023-01-01");
        assertThat(body.get("status")).isEqualTo("Active");
    }

    @Test
    void updateEvent_shouldApplyPartialUpdatesAndRetainExistingFields() throws Exception {
        String badge = "B-" + UUID.randomUUID();
        publishToKafka(badge, new RegisterOfficerRequested(
                badge, "John", "Smith", "Officer",
                "john.smith@example.com", "555-1111", "2023-02-02", "Active"
        ));

        UpdateOfficerRequested update = new UpdateOfficerRequested(
                badge, null, "Doe", "Lieutenant", "john.doe@example.com", null, null
        );
        publishToKafka(badge, update);

        ResponseEntity<Map> response = awaitOfficer(badge);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("badgeNumber")).isEqualTo(badge);
        assertThat(body.get("firstName")).isEqualTo("John"); // unchanged
        assertThat(body.get("lastName")).isEqualTo("Doe");   // updated
        assertThat(body.get("rank")).isEqualTo("Lieutenant");
        assertThat(body.get("email")).isEqualTo("john.doe@example.com");
        assertThat(body.get("phoneNumber")).isEqualTo("555-1111"); // unchanged
        assertThat(body.get("status")).isEqualTo("Active"); // unchanged from register
    }

    @Test
    void changeStatus_shouldUpdateStatusAndAppendHistoryWithoutDuplicates() throws Exception {
        String badge = "B-" + UUID.randomUUID();
        publishToKafka(badge, new RegisterOfficerRequested(
                badge, "Alex", "Ray", "Officer",
                "alex.ray@example.com", "555-2222", "2023-03-03", "Active"
        ));

        publishToKafka(badge, new ChangeOfficerStatusRequested(badge, "Inactive"));
        publishToKafka(badge, new ChangeOfficerStatusRequested(badge, "Inactive")); // duplicate
        publishToKafka(badge, new ChangeOfficerStatusRequested(badge, "OnLeave"));

        ResponseEntity<Map> officerResponse = awaitOfficer(badge);
        assertThat(officerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(officerResponse.getBody()).isNotNull();
        assertThat(officerResponse.getBody().get("status")).isEqualTo("OnLeave");

        ResponseEntity<List> historyResponse = awaitHistory(badge);
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> history = historyResponse.getBody();
        assertThat(history).isNotNull();
        assertThat(history).hasSize(2);
        assertThat(history.get(0).get("status")).isEqualTo("Inactive");
        assertThat(history.get(1).get("status")).isEqualTo("OnLeave");
    }

    @Test
    void natsEvent_shouldNotDoubleProcessWhenKafkaAlsoDelivers() throws Exception {
        String badge = "B-" + UUID.randomUUID();
        RegisterOfficerRequested event = new RegisterOfficerRequested(
                badge, "Nora", "Jet", "Detective",
                "nora.jet@example.com", "555-3333", "2023-04-04", "Active"
        );

        // Publish to Kafka
        publishToKafka(badge, event);

        // Publish same event to NATS subject
        String subject = EventClassification.generateNatsSubject(event);
        try (Connection connection = Nats.connect(nats.getNatsUrl())) {
            connection.publish(subject, objectMapper.writeValueAsBytes(event));
        }

        ResponseEntity<Map> response = awaitOfficer(badge);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("badgeNumber")).isEqualTo(badge);

        // Ensure only one projection row exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM officer_projection WHERE badge_number = ?", Integer.class, badge);
        assertThat(count).isEqualTo(1);
    }

    private void publishToKafka(String key, Object event) throws Exception {
        String payload = objectMapper.writeValueAsString(event);
        producer.send(new ProducerRecord<>("officer-events", key, payload)).get(10, TimeUnit.SECONDS);
    }

    private ResponseEntity<Map> awaitOfficer(String badge) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> restTemplate.getForEntity("/api/projections/officers/{badge}", Map.class, badge),
                        response -> response.getStatusCode().is2xxSuccessful());
    }

    private ResponseEntity<List> awaitHistory(String badge) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> restTemplate.getForEntity("/api/projections/officers/{badge}/history", List.class, badge),
                        response -> response.getStatusCode().is2xxSuccessful());
    }
}
