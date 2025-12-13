package com.knowit.policesystem.projection.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class OperationalProjectionErrorHandlingTest extends IntegrationTestBase {

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
    void getIncident_WithNonExistentId_ReturnsNotFound() {
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/incidents/{id}", Object.class, "NON-EXISTENT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCall_WithNonExistentId_ReturnsNotFound() {
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/calls/{id}", Object.class, "NON-EXISTENT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getDispatch_WithNonExistentId_ReturnsNotFound() {
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/dispatches/{id}", Object.class, "NON-EXISTENT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getActivity_WithNonExistentId_ReturnsNotFound() {
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/activities/{id}", Object.class, "NON-EXISTENT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAssignment_WithNonExistentId_ReturnsNotFound() {
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/assignments/{id}", Object.class, "NON-EXISTENT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getIncidentFull_WithNonExistentId_ReturnsNotFound() {
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/incidents/{id}/full", Object.class, "NON-EXISTENT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listIncidents_WithInvalidPagination_HandlesGracefully() {
        // Test with negative page
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/incidents?page=-1&size=10", Object.class);

        // Should handle gracefully (likely returns page 0)
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST);
    }

    @Test
    void listIncidents_WithLargePageSize_HandlesGracefully() {
        // Test with very large page size
        ResponseEntity<?> response = restTemplate.getForEntity(
                baseUrl + "/incidents?page=0&size=10000", Object.class);

        // Should handle gracefully (likely capped at max size)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
