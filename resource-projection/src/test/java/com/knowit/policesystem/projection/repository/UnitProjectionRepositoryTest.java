package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.units.ChangeUnitStatusRequested;
import com.knowit.policesystem.common.events.units.CreateUnitRequested;
import com.knowit.policesystem.common.events.units.UpdateUnitRequested;
import com.knowit.policesystem.projection.integration.IntegrationTestBase;
import com.knowit.policesystem.projection.model.UnitProjectionEntity;
import com.knowit.policesystem.projection.model.UnitStatusHistoryEntry;
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

class UnitProjectionRepositoryTest extends IntegrationTestBase {

    @Autowired
    private UnitProjectionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE unit_status_history, unit_projection RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE unit_status_history, unit_projection RESTART IDENTITY CASCADE");
    }

    @Test
    void upsert_WithNewUnit_CreatesUnit() {
        String unitId = "UNIT-" + UUID.randomUUID();
        Instant updatedAt = Instant.now();
        CreateUnitRequested event = new CreateUnitRequested(unitId, "Patrol", "Available");

        repository.upsert(event, updatedAt);

        Optional<UnitProjectionEntity> result = repository.findByUnitId(unitId);
        assertThat(result).isPresent();
        UnitProjectionEntity entity = result.get();
        assertThat(entity.unitId()).isEqualTo(unitId);
        assertThat(entity.unitType()).isEqualTo("Patrol");
        assertThat(entity.status()).isEqualTo("Available");
    }

    @Test
    void upsert_WithExistingUnit_UpdatesUnit() {
        String unitId = "UNIT-" + UUID.randomUUID();
        CreateUnitRequested event1 = new CreateUnitRequested(unitId, "Patrol", "Available");
        repository.upsert(event1, Instant.now());

        CreateUnitRequested event2 = new CreateUnitRequested(unitId, "SUV", "In-Use");
        repository.upsert(event2, Instant.now());

        Optional<UnitProjectionEntity> result = repository.findByUnitId(unitId);
        assertThat(result).isPresent();
        UnitProjectionEntity entity = result.get();
        assertThat(entity.unitType()).isEqualTo("SUV");
        assertThat(entity.status()).isEqualTo("In-Use");
    }

    @Test
    void applyUpdate_WithPartialUpdate_PreservesExistingFields() {
        String unitId = "UNIT-" + UUID.randomUUID();
        CreateUnitRequested createEvent = new CreateUnitRequested(unitId, "Patrol", "Available");
        repository.upsert(createEvent, Instant.now());

        UpdateUnitRequested updateEvent = new UpdateUnitRequested(unitId, "SUV", null);
        repository.applyUpdate(updateEvent, Instant.now());

        Optional<UnitProjectionEntity> result = repository.findByUnitId(unitId);
        assertThat(result).isPresent();
        UnitProjectionEntity entity = result.get();
        assertThat(entity.unitType()).isEqualTo("SUV"); // Updated
        assertThat(entity.status()).isEqualTo("Available"); // Preserved
    }

    @Test
    void changeStatus_WithStatusChange_UpdatesStatusAndAddsHistory() {
        String unitId = "UNIT-" + UUID.randomUUID();
        CreateUnitRequested createEvent = new CreateUnitRequested(unitId, "Patrol", "Available");
        repository.upsert(createEvent, Instant.now());

        ChangeUnitStatusRequested statusEvent = new ChangeUnitStatusRequested(unitId, "In-Use");
        boolean changed = repository.changeStatus(statusEvent, Instant.now());

        assertThat(changed).isTrue();
        Optional<UnitProjectionEntity> result = repository.findByUnitId(unitId);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo("In-Use");

        List<UnitStatusHistoryEntry> history = repository.findHistory(unitId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).status()).isEqualTo("In-Use");
    }

    @Test
    void changeStatus_WithSameStatus_DoesNotAddHistory() {
        String unitId = "UNIT-" + UUID.randomUUID();
        CreateUnitRequested createEvent = new CreateUnitRequested(unitId, "Patrol", "Available");
        repository.upsert(createEvent, Instant.now());

        ChangeUnitStatusRequested statusEvent1 = new ChangeUnitStatusRequested(unitId, "In-Use");
        repository.changeStatus(statusEvent1, Instant.now());

        ChangeUnitStatusRequested statusEvent2 = new ChangeUnitStatusRequested(unitId, "In-Use");
        boolean changed = repository.changeStatus(statusEvent2, Instant.now());

        assertThat(changed).isFalse();
        List<UnitStatusHistoryEntry> history = repository.findHistory(unitId);
        assertThat(history).hasSize(1);
    }

    @Test
    void findAll_WithFilters_ReturnsFilteredResults() {
        String unitId1 = "UNIT-" + UUID.randomUUID();
        CreateUnitRequested event1 = new CreateUnitRequested(unitId1, "Patrol", "Available");
        repository.upsert(event1, Instant.now());

        String unitId2 = "UNIT-" + UUID.randomUUID();
        CreateUnitRequested event2 = new CreateUnitRequested(unitId2, "SUV", "In-Use");
        repository.upsert(event2, Instant.now());

        List<UnitProjectionEntity> available = repository.findAll("Available", null, 0, 10);
        assertThat(available).hasSize(1);
        assertThat(available.get(0).unitId()).isEqualTo(unitId1);
    }

    @Test
    void count_WithFilters_ReturnsCorrectCount() {
        String unitId1 = "UNIT-" + UUID.randomUUID();
        CreateUnitRequested event1 = new CreateUnitRequested(unitId1, "Patrol", "Available");
        repository.upsert(event1, Instant.now());

        String unitId2 = "UNIT-" + UUID.randomUUID();
        CreateUnitRequested event2 = new CreateUnitRequested(unitId2, "SUV", "In-Use");
        repository.upsert(event2, Instant.now());

        long totalCount = repository.count(null, null);
        assertThat(totalCount).isEqualTo(2);

        long availableCount = repository.count("Available", null);
        assertThat(availableCount).isEqualTo(1);
    }

    @Test
    void findHistory_ReturnsStatusHistory() {
        String unitId = "UNIT-" + UUID.randomUUID();
        CreateUnitRequested createEvent = new CreateUnitRequested(unitId, "Patrol", "Available");
        repository.upsert(createEvent, Instant.now());

        repository.changeStatus(new ChangeUnitStatusRequested(unitId, "In-Use"), Instant.now());
        repository.changeStatus(new ChangeUnitStatusRequested(unitId, "Maintenance"), Instant.now());

        List<UnitStatusHistoryEntry> history = repository.findHistory(unitId);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).status()).isEqualTo("In-Use");
        assertThat(history.get(1).status()).isEqualTo("Maintenance");
    }
}
