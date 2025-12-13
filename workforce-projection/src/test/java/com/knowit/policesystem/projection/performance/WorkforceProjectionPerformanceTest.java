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

class WorkforceProjectionPerformanceTest extends IntegrationTestBase {

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
        jdbcTemplate.update("DELETE FROM shift_status_history");
        jdbcTemplate.update("DELETE FROM shift_change_projection");
        jdbcTemplate.update("DELETE FROM officer_shift_projection");
        jdbcTemplate.update("DELETE FROM shift_projection");
    }

    @Test
    void apiEndpoint_ResponseTime_MeetsThreshold() {
        // Insert test data
        String shiftId = "SHIFT-" + UUID.randomUUID();
        insertShift(shiftId, Instant.now(), null, "Day", "Active");

        // Measure response time for GET endpoint
        long startTime = System.currentTimeMillis();
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/shifts/{id}", Object.class, shiftId);
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Assert < 500ms for single entity (allowing buffer for test environment)
        assertThat(responseTime).isLessThan(500);
    }

    @Test
    void listEndpoint_WithPagination_PerformsWell() {
        // Insert multiple shifts
        for (int i = 0; i < 10; i++) {
            insertShift("SHIFT-" + UUID.randomUUID(), Instant.now(), null, "Day", "Active");
        }

        // Measure response time for list endpoint
        long startTime = System.currentTimeMillis();
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/shifts?page=0&size=10", Object.class);
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Assert reasonable response time for paginated list
        assertThat(responseTime).isLessThan(1000); // 1 second threshold
    }

    @Test
    void concurrentQueries_HandlesLoad() throws Exception {
        // Insert test data
        String shiftId = "SHIFT-" + UUID.randomUUID();
        insertShift(shiftId, Instant.now(), null, "Day", "Active");

        // Test concurrent queries
        ExecutorService executor = Executors.newFixedThreadPool(10);
        int numberOfQueries = 50;

        CompletableFuture<?>[] futures = IntStream.range(0, numberOfQueries)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    ResponseEntity<?> response = restTemplate.getForEntity(
                            baseUrl + "/shifts/{id}", Object.class, shiftId);
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
    private void insertShift(String shiftId, Instant startTime, Instant endTime, String shiftType, String status) {
        jdbcTemplate.update("""
                INSERT INTO shift_projection (shift_id, start_time, end_time, shift_type, status, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, shiftId, 
                startTime != null ? java.sql.Timestamp.from(startTime) : null,
                endTime != null ? java.sql.Timestamp.from(endTime) : null,
                shiftType, status, java.sql.Timestamp.from(Instant.now()));
    }
}
