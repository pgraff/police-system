package com.knowit.policesystem.projection.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class WorkforceProjectionErrorHandlingTest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/projections";
    }

    @Test
    void getShift_WithNonExistentId_ReturnsNotFound() {
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/shifts/{id}", Object.class, "NON-EXISTENT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getOfficerShift_WithNonExistentId_ReturnsNotFound() {
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/officer-shifts/{id}", Object.class, "999");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getShiftChange_WithNonExistentId_ReturnsNotFound() {
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/shift-changes/{id}", Object.class, "NON-EXISTENT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getShiftHistory_WithNonExistentId_ReturnsNotFound() {
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/shifts/{id}/history", Object.class, "NON-EXISTENT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listShifts_WithInvalidPagination_HandlesGracefully() {
        // Test with negative page
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/shifts?page=-1&size=10", Object.class);

        // Should handle gracefully (likely returns page 0)
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST);
    }

    @Test
    void listShifts_WithLargePageSize_HandlesGracefully() {
        // Test with very large page size
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/shifts?page=0&size=10000", Object.class);

        // Should handle gracefully (likely capped at max size)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listOfficerShifts_WithInvalidPagination_HandlesGracefully() {
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/officer-shifts?page=-1&size=10", Object.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST);
    }

    @Test
    void listShiftChanges_WithInvalidPagination_HandlesGracefully() {
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/shift-changes?page=-1&size=10", Object.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST);
    }
}
