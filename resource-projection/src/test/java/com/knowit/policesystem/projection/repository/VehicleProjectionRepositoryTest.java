package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.vehicles.ChangeVehicleStatusRequested;
import com.knowit.policesystem.common.events.vehicles.RegisterVehicleRequested;
import com.knowit.policesystem.common.events.vehicles.UpdateVehicleRequested;
import com.knowit.policesystem.projection.integration.IntegrationTestBase;
import com.knowit.policesystem.projection.model.VehicleProjectionEntity;
import com.knowit.policesystem.projection.model.VehicleStatusHistoryEntry;
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

class VehicleProjectionRepositoryTest extends IntegrationTestBase {

    @Autowired
    private VehicleProjectionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE vehicle_status_history, vehicle_projection RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE vehicle_status_history, vehicle_projection RESTART IDENTITY CASCADE");
    }

    @Test
    void upsert_WithNewVehicle_CreatesVehicle() {
        String unitId = "UNIT-" + UUID.randomUUID();
        Instant updatedAt = Instant.now();
        RegisterVehicleRequested event = new RegisterVehicleRequested(
                unitId, "Patrol", "ABC-123", "1HGBH41JXMN109186", "Available", "2024-01-15"
        );

        repository.upsert(event, updatedAt);

        Optional<VehicleProjectionEntity> result = repository.findByUnitId(unitId);
        assertThat(result).isPresent();
        VehicleProjectionEntity entity = result.get();
        assertThat(entity.unitId()).isEqualTo(unitId);
        assertThat(entity.vehicleType()).isEqualTo("Patrol");
        assertThat(entity.licensePlate()).isEqualTo("ABC-123");
        assertThat(entity.status()).isEqualTo("Available");
    }

    @Test
    void upsert_WithExistingVehicle_UpdatesVehicle() {
        String unitId = "UNIT-" + UUID.randomUUID();
        RegisterVehicleRequested event1 = new RegisterVehicleRequested(
                unitId, "Patrol", "ABC-123", "1HGBH41JXMN109186", "Available", "2024-01-15"
        );
        repository.upsert(event1, Instant.now());

        RegisterVehicleRequested event2 = new RegisterVehicleRequested(
                unitId, "SUV", "XYZ-789", "1HGBH41JXMN109186", "In-Use", "2024-02-15"
        );
        repository.upsert(event2, Instant.now());

        Optional<VehicleProjectionEntity> result = repository.findByUnitId(unitId);
        assertThat(result).isPresent();
        VehicleProjectionEntity entity = result.get();
        assertThat(entity.vehicleType()).isEqualTo("SUV");
        assertThat(entity.licensePlate()).isEqualTo("XYZ-789");
        assertThat(entity.status()).isEqualTo("In-Use");
    }

    @Test
    void applyUpdate_WithPartialUpdate_PreservesExistingFields() {
        String unitId = "UNIT-" + UUID.randomUUID();
        RegisterVehicleRequested createEvent = new RegisterVehicleRequested(
                unitId, "Patrol", "ABC-123", "1HGBH41JXMN109186", "Available", "2024-01-15"
        );
        repository.upsert(createEvent, Instant.now());

        UpdateVehicleRequested updateEvent = new UpdateVehicleRequested(
                unitId, null, "XYZ-789", null, null, "2024-02-15"
        );
        repository.applyUpdate(updateEvent, Instant.now());

        Optional<VehicleProjectionEntity> result = repository.findByUnitId(unitId);
        assertThat(result).isPresent();
        VehicleProjectionEntity entity = result.get();
        assertThat(entity.vehicleType()).isEqualTo("Patrol"); // Preserved
        assertThat(entity.licensePlate()).isEqualTo("XYZ-789"); // Updated
        assertThat(entity.status()).isEqualTo("Available"); // Preserved
    }

    @Test
    void changeStatus_WithStatusChange_UpdatesStatusAndAddsHistory() {
        String unitId = "UNIT-" + UUID.randomUUID();
        RegisterVehicleRequested createEvent = new RegisterVehicleRequested(
                unitId, "Patrol", "ABC-123", "1HGBH41JXMN109186", "Available", "2024-01-15"
        );
        repository.upsert(createEvent, Instant.now());

        ChangeVehicleStatusRequested statusEvent = new ChangeVehicleStatusRequested(unitId, "In-Use");
        boolean changed = repository.changeStatus(statusEvent, Instant.now());

        assertThat(changed).isTrue();
        Optional<VehicleProjectionEntity> result = repository.findByUnitId(unitId);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo("In-Use");

        List<VehicleStatusHistoryEntry> history = repository.findHistory(unitId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).status()).isEqualTo("In-Use");
    }

    @Test
    void changeStatus_WithSameStatus_DoesNotAddHistory() {
        String unitId = "UNIT-" + UUID.randomUUID();
        RegisterVehicleRequested createEvent = new RegisterVehicleRequested(
                unitId, "Patrol", "ABC-123", "1HGBH41JXMN109186", "Available", "2024-01-15"
        );
        repository.upsert(createEvent, Instant.now());

        ChangeVehicleStatusRequested statusEvent1 = new ChangeVehicleStatusRequested(unitId, "In-Use");
        repository.changeStatus(statusEvent1, Instant.now());

        ChangeVehicleStatusRequested statusEvent2 = new ChangeVehicleStatusRequested(unitId, "In-Use");
        boolean changed = repository.changeStatus(statusEvent2, Instant.now());

        assertThat(changed).isFalse();
        List<VehicleStatusHistoryEntry> history = repository.findHistory(unitId);
        assertThat(history).hasSize(1);
    }

    @Test
    void findAll_WithFilters_ReturnsFilteredResults() {
        String unitId1 = "UNIT-" + UUID.randomUUID();
        RegisterVehicleRequested event1 = new RegisterVehicleRequested(
                unitId1, "Patrol", "ABC-123", "1HGBH41JXMN109186", "Available", "2024-01-15"
        );
        repository.upsert(event1, Instant.now());

        String unitId2 = "UNIT-" + UUID.randomUUID();
        RegisterVehicleRequested event2 = new RegisterVehicleRequested(
                unitId2, "SUV", "XYZ-789", "2HGBH41JXMN109187", "In-Use", "2024-01-15"
        );
        repository.upsert(event2, Instant.now());

        List<VehicleProjectionEntity> available = repository.findAll("Available", null, 0, 10);
        assertThat(available).hasSize(1);
        assertThat(available.get(0).unitId()).isEqualTo(unitId1);
    }

    @Test
    void count_WithFilters_ReturnsCorrectCount() {
        String unitId1 = "UNIT-" + UUID.randomUUID();
        RegisterVehicleRequested event1 = new RegisterVehicleRequested(
                unitId1, "Patrol", "ABC-123", "1HGBH41JXMN109186", "Available", "2024-01-15"
        );
        repository.upsert(event1, Instant.now());

        String unitId2 = "UNIT-" + UUID.randomUUID();
        RegisterVehicleRequested event2 = new RegisterVehicleRequested(
                unitId2, "SUV", "XYZ-789", "2HGBH41JXMN109187", "In-Use", "2024-01-15"
        );
        repository.upsert(event2, Instant.now());

        long totalCount = repository.count(null, null);
        assertThat(totalCount).isEqualTo(2);

        long availableCount = repository.count("Available", null);
        assertThat(availableCount).isEqualTo(1);
    }

    @Test
    void findHistory_ReturnsStatusHistory() {
        String unitId = "UNIT-" + UUID.randomUUID();
        RegisterVehicleRequested createEvent = new RegisterVehicleRequested(
                unitId, "Patrol", "ABC-123", "1HGBH41JXMN109186", "Available", "2024-01-15"
        );
        repository.upsert(createEvent, Instant.now());

        repository.changeStatus(new ChangeVehicleStatusRequested(unitId, "In-Use"), Instant.now());
        repository.changeStatus(new ChangeVehicleStatusRequested(unitId, "Maintenance"), Instant.now());

        List<VehicleStatusHistoryEntry> history = repository.findHistory(unitId);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).status()).isEqualTo("In-Use");
        assertThat(history.get(1).status()).isEqualTo("Maintenance");
    }
}
