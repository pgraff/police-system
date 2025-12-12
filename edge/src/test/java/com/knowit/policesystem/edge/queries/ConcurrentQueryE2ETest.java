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
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for concurrent NATS queries.
 * Verifies that multiple simultaneous queries are handled correctly without race conditions.
 */
class ConcurrentQueryE2ETest extends NatsQueryE2ETestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TopicConfiguration topicConfiguration;

    private ProjectionTestContext projectionContext;
    private Producer<String, String> kafkaProducer;
    private ObjectMapper eventObjectMapper;
    private ExecutorService executorService;

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

        executorService = Executors.newFixedThreadPool(10);

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
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (projectionContext != null) {
            projectionContext.stopAll();
        }
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
    }

    @Test
    void testConcurrentQueries_HandlesMultipleRequests() throws Exception {
        // Given - create multiple officers in projection
        int numOfficers = 10;
        List<String> badgeNumbers = new ArrayList<>();
        
        for (int i = 0; i < numOfficers; i++) {
            String badgeNumber = "BADGE-CONCURRENT-" + UUID.randomUUID();
            badgeNumbers.add(badgeNumber);
            
            RegisterOfficerRequested event = new RegisterOfficerRequested(
                    badgeNumber,
                    "John" + i,
                    "Doe" + i,
                    "Officer",
                    "john" + i + ".doe@police.gov",
                    "555-0" + i,
                    "2020-01-15",
                    "Active"
            );
            
            publishToKafkaAndWait(badgeNumber, event);
        }

        // When - send 10 concurrent requests to edge API
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        
        for (String badgeNumber : badgeNumbers) {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                try {
                    UpdateOfficerRequestDto request = new UpdateOfficerRequestDto(
                            "Updated",
                            "Name",
                            "Sergeant",
                            "updated@police.gov",
                            "555-9999",
                            LocalDate.of(2021, 1, 1)
                    );

                    String requestJson = objectMapper.writeValueAsString(request);
                    int status = mockMvc.perform(put("/api/v1/officers/{badgeNumber}", badgeNumber)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestJson))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                    return status;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorService);
            
            futures.add(future);
        }

        // Then - all requests should complete successfully
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.get(30, TimeUnit.SECONDS);

        List<Integer> statusCodes = new ArrayList<>();
        for (CompletableFuture<Integer> future : futures) {
            statusCodes.add(future.get());
        }

        // All should return 200 OK
        assertThat(statusCodes).hasSize(numOfficers);
        assertThat(statusCodes).allMatch(status -> status == 200);
    }

    @Test
    void testConcurrentQueries_WithMixedExistence_HandlesCorrectly() throws Exception {
        // Given - create some officers, leave some non-existent
        int numExisting = 5;
        int numNonExistent = 5;
        List<String> existingBadgeNumbers = new ArrayList<>();
        List<String> nonExistentBadgeNumbers = new ArrayList<>();

        for (int i = 0; i < numExisting; i++) {
            String badgeNumber = "BADGE-EXISTS-" + UUID.randomUUID();
            existingBadgeNumbers.add(badgeNumber);
            
            RegisterOfficerRequested event = new RegisterOfficerRequested(
                    badgeNumber,
                    "John" + i,
                    "Doe",
                    "Officer",
                    "john" + i + "@police.gov",
                    "555-0" + i,
                    "2020-01-15",
                    "Active"
            );
            
            publishToKafkaAndWait(badgeNumber, event);
        }

        for (int i = 0; i < numNonExistent; i++) {
            nonExistentBadgeNumbers.add("BADGE-NONEXISTENT-" + UUID.randomUUID());
        }

        // When - send concurrent requests for both existing and non-existent officers
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        
        for (String badgeNumber : existingBadgeNumbers) {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                try {
                    UpdateOfficerRequestDto request = new UpdateOfficerRequestDto(
                            "Updated", null, null, null, null, null);
                    String requestJson = objectMapper.writeValueAsString(request);
                    return mockMvc.perform(put("/api/v1/officers/{badgeNumber}", badgeNumber)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestJson))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorService);
            futures.add(future);
        }

        for (String badgeNumber : nonExistentBadgeNumbers) {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                try {
                    UpdateOfficerRequestDto request = new UpdateOfficerRequestDto(
                            "Updated", null, null, null, null, null);
                    String requestJson = objectMapper.writeValueAsString(request);
                    return mockMvc.perform(put("/api/v1/officers/{badgeNumber}", badgeNumber)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestJson))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorService);
            futures.add(future);
        }

        // Then - verify correct status codes
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.get(30, TimeUnit.SECONDS);

        List<Integer> statusCodes = new ArrayList<>();
        for (CompletableFuture<Integer> future : futures) {
            statusCodes.add(future.get());
        }

        // Existing officers should return 200, non-existent should return 404
        assertThat(statusCodes).hasSize(numExisting + numNonExistent);
        assertThat(statusCodes.stream().filter(code -> code == 200).count()).isEqualTo(numExisting);
        assertThat(statusCodes.stream().filter(code -> code == 404).count()).isEqualTo(numNonExistent);
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

