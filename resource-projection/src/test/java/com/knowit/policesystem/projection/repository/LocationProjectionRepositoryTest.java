package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.locations.CreateLocationRequested;
import com.knowit.policesystem.common.events.locations.UpdateLocationRequested;
import com.knowit.policesystem.projection.integration.IntegrationTestBase;
import com.knowit.policesystem.projection.model.LocationProjectionEntity;
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

class LocationProjectionRepositoryTest extends IntegrationTestBase {

    @Autowired
    private LocationProjectionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE location_projection RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE location_projection RESTART IDENTITY CASCADE");
    }

    @Test
    void upsert_WithNewLocation_CreatesLocation() {
        String locationId = "LOC-" + UUID.randomUUID();
        Instant updatedAt = Instant.now();
        CreateLocationRequested event = new CreateLocationRequested(
                locationId, locationId, "123 Main St", "Springfield", "IL", "62701",
                "39.7817", "-89.6501", "Address"
        );

        repository.upsert(event, updatedAt);

        Optional<LocationProjectionEntity> result = repository.findByLocationId(locationId);
        assertThat(result).isPresent();
        LocationProjectionEntity entity = result.get();
        assertThat(entity.locationId()).isEqualTo(locationId);
        assertThat(entity.address()).isEqualTo("123 Main St");
        assertThat(entity.city()).isEqualTo("Springfield");
        assertThat(entity.state()).isEqualTo("IL");
    }

    @Test
    void upsert_WithExistingLocation_UpdatesLocation() {
        String locationId = "LOC-" + UUID.randomUUID();
        CreateLocationRequested event1 = new CreateLocationRequested(
                locationId, locationId, "123 Main St", "Springfield", "IL", "62701",
                "39.7817", "-89.6501", "Address"
        );
        repository.upsert(event1, Instant.now());

        CreateLocationRequested event2 = new CreateLocationRequested(
                locationId, locationId, "456 Oak Ave", "Chicago", "IL", "60601",
                "41.8781", "-87.6298", "Address"
        );
        repository.upsert(event2, Instant.now());

        Optional<LocationProjectionEntity> result = repository.findByLocationId(locationId);
        assertThat(result).isPresent();
        LocationProjectionEntity entity = result.get();
        assertThat(entity.address()).isEqualTo("456 Oak Ave");
        assertThat(entity.city()).isEqualTo("Chicago");
    }

    @Test
    void applyUpdate_WithPartialUpdate_PreservesExistingFields() {
        String locationId = "LOC-" + UUID.randomUUID();
        CreateLocationRequested createEvent = new CreateLocationRequested(
                locationId, locationId, "123 Main St", "Springfield", "IL", "62701",
                "39.7817", "-89.6501", "Address"
        );
        repository.upsert(createEvent, Instant.now());

        UpdateLocationRequested updateEvent = new UpdateLocationRequested(
                locationId, null, "Chicago", null, null, null, null, null
        );
        repository.applyUpdate(updateEvent, Instant.now());

        Optional<LocationProjectionEntity> result = repository.findByLocationId(locationId);
        assertThat(result).isPresent();
        LocationProjectionEntity entity = result.get();
        assertThat(entity.address()).isEqualTo("123 Main St"); // Preserved
        assertThat(entity.city()).isEqualTo("Chicago"); // Updated
        assertThat(entity.state()).isEqualTo("IL"); // Preserved
    }

    @Test
    void findAll_WithFilters_ReturnsFilteredResults() {
        String locationId1 = "LOC-" + UUID.randomUUID();
        CreateLocationRequested event1 = new CreateLocationRequested(
                locationId1, locationId1, "123 Main St", "Springfield", "IL", "62701",
                "39.7817", "-89.6501", "Address"
        );
        repository.upsert(event1, Instant.now());

        String locationId2 = "LOC-" + UUID.randomUUID();
        CreateLocationRequested event2 = new CreateLocationRequested(
                locationId2, locationId2, "456 Oak Ave", "Chicago", "IL", "60601",
                "41.8781", "-87.6298", "Address"
        );
        repository.upsert(event2, Instant.now());

        List<LocationProjectionEntity> springfield = repository.findAll("Springfield", null, 0, 10);
        assertThat(springfield).hasSize(1);
        assertThat(springfield.get(0).locationId()).isEqualTo(locationId1);
    }

    @Test
    void count_WithFilters_ReturnsCorrectCount() {
        String locationId1 = "LOC-" + UUID.randomUUID();
        CreateLocationRequested event1 = new CreateLocationRequested(
                locationId1, locationId1, "123 Main St", "Springfield", "IL", "62701",
                "39.7817", "-89.6501", "Address"
        );
        repository.upsert(event1, Instant.now());

        String locationId2 = "LOC-" + UUID.randomUUID();
        CreateLocationRequested event2 = new CreateLocationRequested(
                locationId2, locationId2, "456 Oak Ave", "Chicago", "IL", "60601",
                "41.8781", "-87.6298", "Address"
        );
        repository.upsert(event2, Instant.now());

        long totalCount = repository.count(null, null);
        assertThat(totalCount).isEqualTo(2);

        long springfieldCount = repository.count("Springfield", null);
        assertThat(springfieldCount).isEqualTo(1);
    }
}
