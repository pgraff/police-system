package com.knowit.policesystem.edge.queries;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.infrastructure.NatsQueryE2ETestBase;
import com.knowit.policesystem.edge.infrastructure.ProjectionTestContext;
import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests for assignment get queries via NATS.
 */
class AssignmentGetQueryE2ETest extends NatsQueryE2ETestBase {

    @Autowired
    private ProjectionQueryService projectionQueryService;

    @Autowired
    private TopicConfiguration topicConfiguration;

    private ProjectionTestContext projectionContext;
    private Producer<String, String> kafkaProducer;
    private ObjectMapper eventObjectMapper;
    private ObjectMapper responseObjectMapper;

    @BeforeEach
    void setUp() throws Exception {
        eventObjectMapper = new ObjectMapper();
        eventObjectMapper.registerModule(new JavaTimeModule());
        eventObjectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        responseObjectMapper = new ObjectMapper();
        responseObjectMapper.registerModule(new JavaTimeModule());
        responseObjectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProducer = new KafkaProducer<>(producerProps);

        projectionContext = new ProjectionTestContext(
                nats.getNatsUrl(),
                kafka.getBootstrapServers(),
                getPostgresJdbcUrl(),
                getPostgresUsername(),
                getPostgresPassword()
        );

        // Use consolidated operational-projection for assignment domain
        boolean started = projectionContext.startProjection("assignment");
        assertThat(started).as("Projection should start successfully").isTrue();

        boolean ready = projectionContext.waitForProjectionReady("assignment", Duration.ofSeconds(30));
        assertThat(ready).as("Projection should be ready").isTrue();
    }

    @AfterEach
    void tearDown() {
        if (projectionContext != null) {
            projectionContext.stopAll();
        }
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
    }

    @Test
    void testGetQuery_WhenAssignmentExists_ReturnsFullData() throws Exception {
        String assignmentId = "ASSIGN-" + UUID.randomUUID();
        CreateAssignmentRequested event = new CreateAssignmentRequested(
                assignmentId,
                assignmentId,
                Instant.now(),
                "Primary",
                "Assigned",
                null,
                null
        );

        publishToKafkaAndWait(assignmentId, event);

        Object data = projectionQueryService.get("assignment", assignmentId);
        assertThat(data).isNotNull();

        Map<String, Object> assignmentData = responseObjectMapper.convertValue(data,
                TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class));

        assertThat(assignmentData.get("assignmentId")).isEqualTo(assignmentId);
        assertThat(assignmentData.get("assignmentType")).isEqualTo("Primary");
        assertThat(assignmentData.get("status")).isEqualTo("Assigned");
    }

    @Test
    void testGetQuery_WhenAssignmentDoesNotExist_ReturnsNull() throws Exception {
        String nonExistentAssignmentId = "ASSIGN-NONEXISTENT-" + UUID.randomUUID();
        Object data = projectionQueryService.get("assignment", nonExistentAssignmentId);
        assertThat(data).isNull();
    }

    private void publishToKafkaAndWait(String key, CreateAssignmentRequested event) throws Exception {
        String payload = eventObjectMapper.writeValueAsString(event);
        kafkaProducer.send(new ProducerRecord<>(topicConfiguration.ASSIGNMENT_EVENTS, key, payload))
                .get(10, TimeUnit.SECONDS);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> true);
        
        Thread.sleep(2000);
    }
}

