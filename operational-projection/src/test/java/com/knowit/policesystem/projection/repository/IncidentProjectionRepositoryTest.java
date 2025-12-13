package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.incidents.ChangeIncidentStatusRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.common.events.incidents.UpdateIncidentRequested;
import com.knowit.policesystem.projection.integration.IntegrationTestBase;
import com.knowit.policesystem.projection.model.IncidentProjectionEntity;
import com.knowit.policesystem.projection.model.IncidentStatusHistoryEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentProjectionRepositoryTest extends IntegrationTestBase {

    @Autowired
    private IncidentProjectionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clean tables between tests
        jdbcTemplate.execute("TRUNCATE incident_status_history, incident_projection RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE incident_status_history, incident_projection RESTART IDENTITY CASCADE");
    }

    @Test
    void upsert_WithNewIncident_CreatesIncident() {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        Instant updatedAt = Instant.now();
        ReportIncidentRequested event = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        );

        repository.upsert(event, updatedAt);

        Optional<IncidentProjectionEntity> result = repository.findByIncidentId(incidentId);
        assertThat(result).isPresent();
        IncidentProjectionEntity entity = result.get();
        assertThat(entity.incidentId()).isEqualTo(incidentId);
        assertThat(entity.incidentNumber()).isEqualTo("2024-001");
        assertThat(entity.priority()).isEqualTo("High");
        assertThat(entity.status()).isEqualTo("Reported");
        assertThat(entity.description()).isEqualTo("Test incident");
        assertThat(entity.incidentType()).isEqualTo("Traffic");
    }

    @Test
    void upsert_WithExistingIncident_UpdatesIncident() {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        ReportIncidentRequested event1 = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                reportedTime, "Initial description", "Traffic"
        );
        repository.upsert(event1, Instant.now());

        ReportIncidentRequested event2 = new ReportIncidentRequested(
                incidentId, "2024-002", "Medium", "Active",
                reportedTime, "Updated description", "Emergency"
        );
        repository.upsert(event2, Instant.now());

        Optional<IncidentProjectionEntity> result = repository.findByIncidentId(incidentId);
        assertThat(result).isPresent();
        IncidentProjectionEntity entity = result.get();
        assertThat(entity.incidentNumber()).isEqualTo("2024-002");
        assertThat(entity.priority()).isEqualTo("Medium");
        assertThat(entity.status()).isEqualTo("Active");
        assertThat(entity.description()).isEqualTo("Updated description");
    }

    @Test
    void applyUpdate_WithPartialUpdate_PreservesExistingFields() {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        ReportIncidentRequested createEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                reportedTime, "Initial description", "Traffic"
        );
        repository.upsert(createEvent, Instant.now());

        UpdateIncidentRequested updateEvent = new UpdateIncidentRequested(
                incidentId, "Medium", "Updated description", "Emergency"
        );
        repository.applyUpdate(updateEvent, Instant.now());

        Optional<IncidentProjectionEntity> result = repository.findByIncidentId(incidentId);
        assertThat(result).isPresent();
        IncidentProjectionEntity entity = result.get();
        assertThat(entity.incidentNumber()).isEqualTo("2024-001"); // Preserved
        assertThat(entity.priority()).isEqualTo("Medium"); // Updated
        assertThat(entity.status()).isEqualTo("Reported"); // Preserved
        assertThat(entity.description()).isEqualTo("Updated description"); // Updated
        assertThat(entity.incidentType()).isEqualTo("Emergency"); // Updated
    }

    @Test
    void changeStatus_WithStatusChange_UpdatesStatusAndAddsHistory() {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        ReportIncidentRequested createEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        );
        repository.upsert(createEvent, Instant.now());

        ChangeIncidentStatusRequested statusEvent = new ChangeIncidentStatusRequested(
                incidentId, "Active"
        );
        boolean changed = repository.changeStatus(statusEvent, Instant.now());

        assertThat(changed).isTrue();
        Optional<IncidentProjectionEntity> result = repository.findByIncidentId(incidentId);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo("Active");

        List<IncidentStatusHistoryEntry> history = repository.findHistory(incidentId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).status()).isEqualTo("Active");
    }

    @Test
    void changeStatus_WithSameStatus_DoesNotAddHistory() {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        ReportIncidentRequested createEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        );
        repository.upsert(createEvent, Instant.now());

        ChangeIncidentStatusRequested statusEvent1 = new ChangeIncidentStatusRequested(
                incidentId, "Active"
        );
        repository.changeStatus(statusEvent1, Instant.now());

        ChangeIncidentStatusRequested statusEvent2 = new ChangeIncidentStatusRequested(
                incidentId, "Active"
        );
        boolean changed = repository.changeStatus(statusEvent2, Instant.now());

        assertThat(changed).isFalse();
        List<IncidentStatusHistoryEntry> history = repository.findHistory(incidentId);
        assertThat(history).hasSize(1); // Only one entry
    }

    @Test
    void updateDispatchTime_UpdatesDispatchedTime() {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        ReportIncidentRequested createEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        );
        repository.upsert(createEvent, Instant.now());

        Instant dispatchedTime = Instant.now();
        repository.updateDispatchTime(incidentId, dispatchedTime, Instant.now());

        Optional<IncidentProjectionEntity> result = repository.findByIncidentId(incidentId);
        assertThat(result).isPresent();
        assertThat(result.get().dispatchedTime()).isNotNull();
    }

    @Test
    void updateArrivedTime_UpdatesArrivedTime() {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        ReportIncidentRequested createEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        );
        repository.upsert(createEvent, Instant.now());

        Instant arrivedTime = Instant.now();
        repository.updateArrivedTime(incidentId, arrivedTime, Instant.now());

        Optional<IncidentProjectionEntity> result = repository.findByIncidentId(incidentId);
        assertThat(result).isPresent();
        assertThat(result.get().arrivedTime()).isNotNull();
    }

    @Test
    void updateClearedTime_UpdatesClearedTime() {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        ReportIncidentRequested createEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        );
        repository.upsert(createEvent, Instant.now());

        Instant clearedTime = Instant.now();
        repository.updateClearedTime(incidentId, clearedTime, Instant.now());

        Optional<IncidentProjectionEntity> result = repository.findByIncidentId(incidentId);
        assertThat(result).isPresent();
        assertThat(result.get().clearedTime()).isNotNull();
    }

    @Test
    void findAll_WithFilters_ReturnsFilteredResults() {
        // Create test data
        String incidentId1 = "INC-" + UUID.randomUUID();
        ReportIncidentRequested event1 = new ReportIncidentRequested(
                incidentId1, "2024-001", "High", "Reported",
                Instant.now(), "Test 1", "Traffic"
        );
        repository.upsert(event1, Instant.now());

        String incidentId2 = "INC-" + UUID.randomUUID();
        ReportIncidentRequested event2 = new ReportIncidentRequested(
                incidentId2, "2024-002", "Medium", "Active",
                Instant.now(), "Test 2", "Emergency"
        );
        repository.upsert(event2, Instant.now());

        // Test filtering
        List<IncidentProjectionEntity> highPriority = repository.findAll("Reported", "High", null, 0, 10);
        assertThat(highPriority).hasSize(1);
        assertThat(highPriority.get(0).incidentId()).isEqualTo(incidentId1);

        List<IncidentProjectionEntity> trafficType = repository.findAll(null, null, "Traffic", 0, 10);
        assertThat(trafficType).hasSize(1);
        assertThat(trafficType.get(0).incidentId()).isEqualTo(incidentId1);
    }

    @Test
    void count_WithFilters_ReturnsCorrectCount() {
        // Create test data
        String incidentId1 = "INC-" + UUID.randomUUID();
        ReportIncidentRequested event1 = new ReportIncidentRequested(
                incidentId1, "2024-001", "High", "Reported",
                Instant.now(), "Test 1", "Traffic"
        );
        repository.upsert(event1, Instant.now());

        String incidentId2 = "INC-" + UUID.randomUUID();
        ReportIncidentRequested event2 = new ReportIncidentRequested(
                incidentId2, "2024-002", "Medium", "Active",
                Instant.now(), "Test 2", "Emergency"
        );
        repository.upsert(event2, Instant.now());

        long totalCount = repository.count(null, null, null);
        assertThat(totalCount).isEqualTo(2);

        long highPriorityCount = repository.count("Reported", "High", null);
        assertThat(highPriorityCount).isEqualTo(1);
    }

    @Test
    void findHistory_ReturnsStatusHistory() {
        String incidentId = "INC-" + UUID.randomUUID();
        Instant reportedTime = Instant.now();
        ReportIncidentRequested createEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                reportedTime, "Test incident", "Traffic"
        );
        repository.upsert(createEvent, Instant.now());

        repository.changeStatus(new ChangeIncidentStatusRequested(incidentId, "Active"), Instant.now());
        repository.changeStatus(new ChangeIncidentStatusRequested(incidentId, "Resolved"), Instant.now());

        List<IncidentStatusHistoryEntry> history = repository.findHistory(incidentId);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).status()).isEqualTo("Active");
        assertThat(history.get(1).status()).isEqualTo("Resolved");
    }
}
