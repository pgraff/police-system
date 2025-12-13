package com.knowit.policesystem.projection.integration;

import com.knowit.policesystem.projection.api.ShiftProjectionPageResponse;
import com.knowit.policesystem.projection.api.ShiftProjectionResponse;
import com.knowit.policesystem.projection.api.ShiftStatusHistoryResponse;
import com.knowit.policesystem.projection.api.OfficerShiftProjectionPageResponse;
import com.knowit.policesystem.projection.api.OfficerShiftProjectionResponse;
import com.knowit.policesystem.projection.api.ShiftChangeProjectionPageResponse;
import com.knowit.policesystem.projection.api.ShiftChangeProjectionResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

class WorkforceProjectionApiIntegrationTest extends IntegrationTestBase {

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

    // Shift endpoint tests

    @Test
    void getShift_WithValidId_ReturnsOk() {
        String shiftId = "SHIFT-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(28800); // 8 hours
        insertShift(shiftId, startTime, endTime, "Day", "Active");

        ResponseEntity<ShiftProjectionResponse> response = restTemplate.getForEntity(
                baseUrl + "/shifts/{id}", ShiftProjectionResponse.class, shiftId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().shiftId()).isEqualTo(shiftId);
        assertThat(response.getBody().shiftType()).isEqualTo("Day");
        assertThat(response.getBody().status()).isEqualTo("Active");
    }

    @Test
    void getShift_WithInvalidId_ReturnsNotFound() {
        ResponseEntity<ShiftProjectionResponse> response = restTemplate.getForEntity(
                baseUrl + "/shifts/{id}", ShiftProjectionResponse.class, "INVALID");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getShiftHistory_WithValidId_ReturnsOk() {
        String shiftId = "SHIFT-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        insertShift(shiftId, startTime, null, "Day", "Active");
        insertShiftStatusHistory(shiftId, "Active", Instant.now(), "EVENT-001");
        insertShiftStatusHistory(shiftId, "Completed", Instant.now().plusSeconds(28800), "EVENT-002");

        ResponseEntity<java.util.List<ShiftStatusHistoryResponse>> response = restTemplate.exchange(
                baseUrl + "/shifts/{id}/history",
                org.springframework.http.HttpMethod.GET,
                null,
                new org.springframework.core.ParameterizedTypeReference<java.util.List<ShiftStatusHistoryResponse>>() {},
                shiftId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void listShifts_WithFilters_ReturnsFilteredResults() {
        String shiftId1 = "SHIFT-" + UUID.randomUUID();
        String shiftId2 = "SHIFT-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        insertShift(shiftId1, startTime, null, "Day", "Active");
        insertShift(shiftId2, startTime, null, "Night", "Active");

        ResponseEntity<ShiftProjectionPageResponse> response = restTemplate.getForEntity(
                baseUrl + "/shifts?status=Active&shiftType=Day", ShiftProjectionPageResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(1);
        assertThat(response.getBody().content().get(0).shiftId()).isEqualTo(shiftId1);
    }

    @Test
    void listShifts_WithPagination_ReturnsPaginatedResults() {
        // Insert multiple shifts
        for (int i = 0; i < 5; i++) {
            insertShift("SHIFT-" + UUID.randomUUID(), Instant.now(), null, "Day", "Active");
        }

        ResponseEntity<ShiftProjectionPageResponse> response = restTemplate.getForEntity(
                baseUrl + "/shifts?page=0&size=2", ShiftProjectionPageResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSizeLessThanOrEqualTo(2);
        assertThat(response.getBody().total()).isGreaterThanOrEqualTo(5);
    }

    // Officer shift endpoint tests

    @Test
    void getOfficerShift_WithValidId_ReturnsOk() {
        String shiftId = "SHIFT-" + UUID.randomUUID();
        String badgeNumber = "BADGE-001";
        Long officerShiftId = 1L;
        Instant checkInTime = Instant.now();
        insertShift(shiftId, Instant.now(), null, "Day", "Active");
        insertOfficerShift(officerShiftId, shiftId, badgeNumber, checkInTime, null, "Regular");

        ResponseEntity<OfficerShiftProjectionResponse> response = restTemplate.getForEntity(
                baseUrl + "/officer-shifts/{id}", OfficerShiftProjectionResponse.class, officerShiftId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().shiftId()).isEqualTo(shiftId);
        assertThat(response.getBody().badgeNumber()).isEqualTo(badgeNumber);
    }

    @Test
    void listOfficerShifts_WithFilters_ReturnsFilteredResults() {
        String shiftId = "SHIFT-" + UUID.randomUUID();
        String badgeNumber1 = "BADGE-001";
        String badgeNumber2 = "BADGE-002";
        insertShift(shiftId, Instant.now(), null, "Day", "Active");
        insertOfficerShift(1L, shiftId, badgeNumber1, Instant.now(), null, "Regular");
        insertOfficerShift(2L, shiftId, badgeNumber2, Instant.now(), null, "Supervisor");

        ResponseEntity<OfficerShiftProjectionPageResponse> response = restTemplate.getForEntity(
                baseUrl + "/officer-shifts?shiftId=" + shiftId, OfficerShiftProjectionPageResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(2);
    }

    // Shift change endpoint tests

    @Test
    void getShiftChange_WithValidId_ReturnsOk() {
        String shiftId = "SHIFT-" + UUID.randomUUID();
        String shiftChangeId = "CHANGE-" + UUID.randomUUID();
        Instant changeTime = Instant.now();
        insertShift(shiftId, Instant.now(), null, "Day", "Active");
        insertShiftChange(shiftChangeId, shiftId, changeTime, "Briefing", "Shift change notes");

        ResponseEntity<ShiftChangeProjectionResponse> response = restTemplate.getForEntity(
                baseUrl + "/shift-changes/{id}", ShiftChangeProjectionResponse.class, shiftChangeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().shiftChangeId()).isEqualTo(shiftChangeId);
        assertThat(response.getBody().shiftId()).isEqualTo(shiftId);
        assertThat(response.getBody().changeType()).isEqualTo("Briefing");
    }

    @Test
    void listShiftChanges_WithFilters_ReturnsFilteredResults() {
        String shiftId = "SHIFT-" + UUID.randomUUID();
        String shiftChangeId1 = "CHANGE-" + UUID.randomUUID();
        String shiftChangeId2 = "CHANGE-" + UUID.randomUUID();
        insertShift(shiftId, Instant.now(), null, "Day", "Active");
        insertShiftChange(shiftChangeId1, shiftId, Instant.now(), "Briefing", "Notes 1");
        insertShiftChange(shiftChangeId2, shiftId, Instant.now(), "Handoff", "Notes 2");

        ResponseEntity<ShiftChangeProjectionPageResponse> response = restTemplate.getForEntity(
                baseUrl + "/shift-changes?shiftId=" + shiftId, ShiftChangeProjectionPageResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(2);
    }

    // Helper methods for inserting test data

    private void insertShift(String shiftId, Instant startTime, Instant endTime, String shiftType, String status) {
        jdbcTemplate.update("""
                INSERT INTO shift_projection (shift_id, start_time, end_time, shift_type, status, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, shiftId, 
                startTime != null ? java.sql.Timestamp.from(startTime) : null,
                endTime != null ? java.sql.Timestamp.from(endTime) : null,
                shiftType, status, java.sql.Timestamp.from(Instant.now()));
    }

    private void insertShiftStatusHistory(String shiftId, String status, Instant changedAt, String eventId) {
        jdbcTemplate.update("""
                INSERT INTO shift_status_history (shift_id, status, changed_at, event_id)
                VALUES (?, ?, ?, ?)
                """, shiftId, status, java.sql.Timestamp.from(changedAt), eventId);
    }

    private void insertOfficerShift(Long id, String shiftId, String badgeNumber, Instant checkInTime, 
                                    Instant checkOutTime, String shiftRoleType) {
        jdbcTemplate.update("""
                INSERT INTO officer_shift_projection (id, shift_id, badge_number, check_in_time, check_out_time, shift_role_type, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, shiftId, badgeNumber,
                checkInTime != null ? java.sql.Timestamp.from(checkInTime) : null,
                checkOutTime != null ? java.sql.Timestamp.from(checkOutTime) : null,
                shiftRoleType, java.sql.Timestamp.from(Instant.now()));
    }

    private void insertShiftChange(String shiftChangeId, String shiftId, Instant changeTime, 
                                  String changeType, String notes) {
        jdbcTemplate.update("""
                INSERT INTO shift_change_projection (shift_change_id, shift_id, change_time, change_type, notes, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, shiftChangeId, shiftId, java.sql.Timestamp.from(changeTime), 
                changeType, notes, java.sql.Timestamp.from(Instant.now()));
    }
}
