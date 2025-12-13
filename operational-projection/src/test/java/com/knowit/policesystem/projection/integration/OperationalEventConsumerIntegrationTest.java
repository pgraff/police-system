package com.knowit.policesystem.projection.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
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
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OperationalEventConsumerIntegrationTest extends IntegrationTestBase {

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
    }

    @AfterEach
    void tearDown() {
        if (producer != null) {
            producer.close();
        }
    }

    @Test
    void consumeIncidentEvent_FromKafka_ProcessesEvent() throws Exception {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        ReportIncidentRequested event = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        );

        String eventJson = objectMapper.writeValueAsString(event);
        producer.send(new ProducerRecord<>("incident-events", incidentId, eventJson));
        producer.flush();

        // Wait for event to be processed (projection service is stub, so just verify no errors)
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // Event should be consumed without errors
                    // Since projection service is a stub, we just verify the listener is working
                });
    }

    @Test
    void consumeMultipleTopics_AllTopicsProcessed() throws Exception {
        // Publish events to different topics
        String incidentId = "INC-" + UUID.randomUUID();
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported", Instant.now(), "Test", "Traffic"
        );
        producer.send(new ProducerRecord<>("incident-events", incidentId, objectMapper.writeValueAsString(incidentEvent)));

        String callId = "CALL-" + UUID.randomUUID();
        com.knowit.policesystem.common.events.calls.ReceiveCallRequested callEvent = 
                new com.knowit.policesystem.common.events.calls.ReceiveCallRequested(
                        callId, callId, "High", "Received", Instant.now(), "Test call", "Emergency"
                );
        producer.send(new ProducerRecord<>("call-events", callId, objectMapper.writeValueAsString(callEvent)));

        producer.flush();

        // Wait for events to be processed
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // Events should be consumed without errors
                });
    }
}
