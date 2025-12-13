package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.officers.ChangeOfficerStatusRequested;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.common.events.officers.UpdateOfficerRequested;
import com.knowit.policesystem.projection.integration.IntegrationTestBase;
import com.knowit.policesystem.projection.model.OfficerProjectionEntity;
import com.knowit.policesystem.projection.model.OfficerStatusHistoryEntry;
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

class OfficerProjectionRepositoryTest extends IntegrationTestBase {

    @Autowired
    private OfficerProjectionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE officer_status_history, officer_projection RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE officer_status_history, officer_projection RESTART IDENTITY CASCADE");
    }

    @Test
    void upsert_WithNewOfficer_CreatesOfficer() {
        String badgeNumber = "BADGE-" + UUID.randomUUID();
        Instant updatedAt = Instant.now();
        RegisterOfficerRequested event = new RegisterOfficerRequested(
                badgeNumber, "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active"
        );

        repository.upsert(event, updatedAt);

        Optional<OfficerProjectionEntity> result = repository.findByBadgeNumber(badgeNumber);
        assertThat(result).isPresent();
        OfficerProjectionEntity entity = result.get();
        assertThat(entity.badgeNumber()).isEqualTo(badgeNumber);
        assertThat(entity.firstName()).isEqualTo("John");
        assertThat(entity.lastName()).isEqualTo("Doe");
        assertThat(entity.rank()).isEqualTo("Sergeant");
        assertThat(entity.status()).isEqualTo("Active");
    }

    @Test
    void upsert_WithExistingOfficer_UpdatesOfficer() {
        String badgeNumber = "BADGE-" + UUID.randomUUID();
        RegisterOfficerRequested event1 = new RegisterOfficerRequested(
                badgeNumber, "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active"
        );
        repository.upsert(event1, Instant.now());

        RegisterOfficerRequested event2 = new RegisterOfficerRequested(
                badgeNumber, "Jane", "Smith", "Lieutenant", "jane.smith@police.gov",
                "555-0200", "2020-01-15", "On-Duty"
        );
        repository.upsert(event2, Instant.now());

        Optional<OfficerProjectionEntity> result = repository.findByBadgeNumber(badgeNumber);
        assertThat(result).isPresent();
        OfficerProjectionEntity entity = result.get();
        assertThat(entity.firstName()).isEqualTo("Jane");
        assertThat(entity.lastName()).isEqualTo("Smith");
        assertThat(entity.rank()).isEqualTo("Lieutenant");
        assertThat(entity.status()).isEqualTo("On-Duty");
    }

    @Test
    void applyUpdate_WithPartialUpdate_PreservesExistingFields() {
        String badgeNumber = "BADGE-" + UUID.randomUUID();
        RegisterOfficerRequested createEvent = new RegisterOfficerRequested(
                badgeNumber, "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active"
        );
        repository.upsert(createEvent, Instant.now());

        UpdateOfficerRequested updateEvent = new UpdateOfficerRequested(
                badgeNumber, null, "Smith", "Lieutenant", null, null, null
        );
        repository.applyUpdate(updateEvent, Instant.now());

        Optional<OfficerProjectionEntity> result = repository.findByBadgeNumber(badgeNumber);
        assertThat(result).isPresent();
        OfficerProjectionEntity entity = result.get();
        assertThat(entity.firstName()).isEqualTo("John"); // Preserved
        assertThat(entity.lastName()).isEqualTo("Smith"); // Updated
        assertThat(entity.rank()).isEqualTo("Lieutenant"); // Updated
        assertThat(entity.status()).isEqualTo("Active"); // Preserved
    }

    @Test
    void changeStatus_WithStatusChange_UpdatesStatusAndAddsHistory() {
        String badgeNumber = "BADGE-" + UUID.randomUUID();
        RegisterOfficerRequested createEvent = new RegisterOfficerRequested(
                badgeNumber, "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active"
        );
        repository.upsert(createEvent, Instant.now());

        ChangeOfficerStatusRequested statusEvent = new ChangeOfficerStatusRequested(badgeNumber, "On-Duty");
        boolean changed = repository.changeStatus(statusEvent, Instant.now());

        assertThat(changed).isTrue();
        Optional<OfficerProjectionEntity> result = repository.findByBadgeNumber(badgeNumber);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo("On-Duty");

        List<OfficerStatusHistoryEntry> history = repository.findHistory(badgeNumber);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).status()).isEqualTo("On-Duty");
    }

    @Test
    void changeStatus_WithSameStatus_DoesNotAddHistory() {
        String badgeNumber = "BADGE-" + UUID.randomUUID();
        RegisterOfficerRequested createEvent = new RegisterOfficerRequested(
                badgeNumber, "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active"
        );
        repository.upsert(createEvent, Instant.now());

        ChangeOfficerStatusRequested statusEvent1 = new ChangeOfficerStatusRequested(badgeNumber, "On-Duty");
        repository.changeStatus(statusEvent1, Instant.now());

        ChangeOfficerStatusRequested statusEvent2 = new ChangeOfficerStatusRequested(badgeNumber, "On-Duty");
        boolean changed = repository.changeStatus(statusEvent2, Instant.now());

        assertThat(changed).isFalse();
        List<OfficerStatusHistoryEntry> history = repository.findHistory(badgeNumber);
        assertThat(history).hasSize(1); // Only one entry
    }

    @Test
    void findAll_WithFilters_ReturnsFilteredResults() {
        String badgeNumber1 = "BADGE-" + UUID.randomUUID();
        RegisterOfficerRequested event1 = new RegisterOfficerRequested(
                badgeNumber1, "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active"
        );
        repository.upsert(event1, Instant.now());

        String badgeNumber2 = "BADGE-" + UUID.randomUUID();
        RegisterOfficerRequested event2 = new RegisterOfficerRequested(
                badgeNumber2, "Jane", "Smith", "Lieutenant", "jane.smith@police.gov",
                "555-0200", "2020-01-15", "On-Duty"
        );
        repository.upsert(event2, Instant.now());

        List<OfficerProjectionEntity> active = repository.findAll("Active", null, 0, 10);
        assertThat(active).hasSize(1);
        assertThat(active.get(0).badgeNumber()).isEqualTo(badgeNumber1);

        List<OfficerProjectionEntity> sergeants = repository.findAll(null, "Sergeant", 0, 10);
        assertThat(sergeants).hasSize(1);
        assertThat(sergeants.get(0).badgeNumber()).isEqualTo(badgeNumber1);
    }

    @Test
    void count_WithFilters_ReturnsCorrectCount() {
        String badgeNumber1 = "BADGE-" + UUID.randomUUID();
        RegisterOfficerRequested event1 = new RegisterOfficerRequested(
                badgeNumber1, "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active"
        );
        repository.upsert(event1, Instant.now());

        String badgeNumber2 = "BADGE-" + UUID.randomUUID();
        RegisterOfficerRequested event2 = new RegisterOfficerRequested(
                badgeNumber2, "Jane", "Smith", "Lieutenant", "jane.smith@police.gov",
                "555-0200", "2020-01-15", "On-Duty"
        );
        repository.upsert(event2, Instant.now());

        long totalCount = repository.count(null, null);
        assertThat(totalCount).isEqualTo(2);

        long activeCount = repository.count("Active", null);
        assertThat(activeCount).isEqualTo(1);
    }

    @Test
    void findHistory_ReturnsStatusHistory() {
        String badgeNumber = "BADGE-" + UUID.randomUUID();
        RegisterOfficerRequested createEvent = new RegisterOfficerRequested(
                badgeNumber, "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active"
        );
        repository.upsert(createEvent, Instant.now());

        repository.changeStatus(new ChangeOfficerStatusRequested(badgeNumber, "On-Duty"), Instant.now());
        repository.changeStatus(new ChangeOfficerStatusRequested(badgeNumber, "Off-Duty"), Instant.now());

        List<OfficerStatusHistoryEntry> history = repository.findHistory(badgeNumber);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).status()).isEqualTo("On-Duty");
        assertThat(history.get(1).status()).isEqualTo("Off-Duty");
    }
}
