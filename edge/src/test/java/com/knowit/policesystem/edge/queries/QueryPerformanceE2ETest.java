package com.knowit.policesystem.edge.queries;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.domain.OfficerStatus;
import com.knowit.policesystem.edge.dto.UpdateOfficerRequestDto;
import com.knowit.policesystem.edge.infrastructure.NatsQueryE2ETestBase;
import com.knowit.policesystem.edge.infrastructure.ProjectionTestContext;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for NATS query performance.
 * Measures query latency and verifies SLA compliance.
 */
class QueryPerformanceE2ETest extends NatsQueryE2ETestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TopicConfiguration topicConfiguration;

    private ProjectionTestContext projectionContext;
    private Producer<String, String> kafkaProducer;
    private ObjectMapper eventObjectMapper;

    @BeforeEach
    void setUp() throws Exception {
        eventObjectMapper = new ObjectMapper();
        eventObjectMapper.registerModule(new JavaTimeModule());
        eventObjectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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

        boolean started = projectionContext.startProjection("officer", "com.knowit.policesystem.projection.OfficerProjectionApplication");
        assertThat(started).as("Projection should start successfully").isTrue();

        boolean ready = projectionContext.waitForProjectionReady("officer", Duration.ofSeconds(30));
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
    void testQueryLatency_MeetsSLA() throws Exception {
        // Given - create an officer in the projection
        String badgeNumber = "BADGE-PERF-" + UUID.randomUUID();
        RegisterOfficerRequested event = new RegisterOfficerRequested(
                badgeNumber,
                "John",
                "Doe",
                "Officer",
                "john.doe@police.gov",
                "555-0100",
                "2020-01-15",
                "Active"
        );

        publishToKafkaAndWait(badgeNumber, event);

        // When - perform multiple queries and measure latency
        int numQueries = 20;
        List<Long> latencies = new ArrayList<>();

        UpdateOfficerRequestDto request = new UpdateOfficerRequestDto(
                "Jane",
                "Smith",
                "Sergeant",
                "jane.smith@police.gov",
                "555-0200",
                LocalDate.of(2021, 3, 20)
        );

        String requestJson = objectMapper.writeValueAsString(request);

        for (int i = 0; i < numQueries; i++) {
            long startTime = System.currentTimeMillis();
            
            mockMvc.perform(put("/api/v1/officers/{badgeNumber}", badgeNumber)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());
            
            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            latencies.add(latency);
        }

        // Then - verify SLA compliance
        Collections.sort(latencies);
        
        // Calculate average
        double average = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        // Calculate 95th percentile
        int p95Index = (int) Math.ceil(latencies.size() * 0.95) - 1;
        long p95Latency = latencies.get(Math.max(0, p95Index));

        System.out.println("Query Performance Metrics:");
        System.out.println("  Average latency: " + average + "ms");
        System.out.println("  95th percentile: " + p95Latency + "ms");
        System.out.println("  Min latency: " + Collections.min(latencies) + "ms");
        System.out.println("  Max latency: " + Collections.max(latencies) + "ms");

        // Verify SLA: Average < 100ms, 95th percentile < 200ms
        assertThat(average).as("Average query latency should be < 100ms").isLessThan(100.0);
        assertThat(p95Latency).as("95th percentile query latency should be < 200ms").isLessThan(200L);
    }

    private void publishToKafkaAndWait(String key, RegisterOfficerRequested event) throws Exception {
        String payload = eventObjectMapper.writeValueAsString(event);
        kafkaProducer.send(new ProducerRecord<>(topicConfiguration.OFFICER_EVENTS, key, payload))
                .get(10, TimeUnit.SECONDS);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> true);
        
        Thread.sleep(2000);
    }
}

