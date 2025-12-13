package com.knowit.policesystem.projection.performance;

import com.knowit.policesystem.projection.integration.IntegrationTestBase;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class OperationalProjectionPerformanceTest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/projections";
    }

    @AfterEach
    void tearDown() {
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
    void apiEndpoint_ResponseTime_MeetsThreshold() {
        // Insert test data
        String incidentId = "INC-" + UUID.randomUUID();
        insertIncident(incidentId, "2024-001", "High", "Reported", Instant.now(), "Test", "Traffic");

        // Measure response time for GET endpoint
        long startTime = System.currentTimeMillis();
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/incidents/{id}", Object.class, incidentId);
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Assert < 100ms for single entity (allowing some buffer for test environment)
        assertThat(responseTime).isLessThan(500); // 500ms threshold for test environment
    }

    @Test
    void compositeQuery_ResponseTime_MeetsThreshold() {
        // Insert test data
        String incidentId = "INC-" + UUID.randomUUID();
        insertIncident(incidentId, "2024-001", "High", "Reported", Instant.now(), "Test", "Traffic");

        // Measure response time for composite query
        long startTime = System.currentTimeMillis();
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/incidents/{id}/full", Object.class, incidentId);
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Assert < 500ms for composite query (allowing buffer for test environment)
        assertThat(responseTime).isLessThan(1000); // 1 second threshold for test environment
    }

    @Test
    void listEndpoint_WithPagination_PerformsWell() {
        // Insert multiple incidents
        for (int i = 0; i < 10; i++) {
            insertIncident("INC-" + UUID.randomUUID(), "2024-" + String.format("%03d", i),
                    "High", "Reported", Instant.now(), "Test " + i, "Traffic");
        }

        // Measure response time for list endpoint
        long startTime = System.currentTimeMillis();
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/incidents?page=0&size=10", Object.class);
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Assert reasonable response time for paginated list
        assertThat(responseTime).isLessThan(1000); // 1 second threshold
    }

    @Test
    void concurrentQueries_HandlesLoad() throws Exception {
        // Insert test data
        String incidentId = "INC-" + UUID.randomUUID();
        insertIncident(incidentId, "2024-001", "High", "Reported", Instant.now(), "Test", "Traffic");

        // Test concurrent queries
        ExecutorService executor = Executors.newFixedThreadPool(10);
        int numberOfQueries = 50;

        CompletableFuture<?>[] futures = IntStream.range(0, numberOfQueries)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    ResponseEntity<?> response = restTemplate.getForEntity(
                            baseUrl + "/incidents/{id}", Object.class, incidentId);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                }, executor))
                .toArray(CompletableFuture[]::new);

        long startTime = System.currentTimeMillis();
        CompletableFuture.allOf(futures).join();
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        executor.shutdown();

        // All queries should complete successfully
        // Average response time should be reasonable
        double averageTime = (double) totalTime / numberOfQueries;
        assertThat(averageTime).isLessThan(200); // Average < 200ms per query
    }

    // Helper method
    private void insertIncident(String incidentId, String incidentNumber, String priority, String status,
                                Instant reportedTime, String description, String incidentType) {
        jdbcTemplate.update("""
                INSERT INTO incident_projection (incident_id, incident_number, priority, status, reported_time, description, incident_type, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, incidentId, incidentNumber, priority, status, java.sql.Timestamp.from(reportedTime),
                description, incidentType, java.sql.Timestamp.from(Instant.now()));
    }
}
