package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.persons.RegisterPersonRequested;
import com.knowit.policesystem.common.events.persons.UpdatePersonRequested;
import com.knowit.policesystem.projection.integration.IntegrationTestBase;
import com.knowit.policesystem.projection.model.PersonProjectionEntity;
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

class PersonProjectionRepositoryTest extends IntegrationTestBase {

    @Autowired
    private PersonProjectionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE person_projection RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE person_projection RESTART IDENTITY CASCADE");
    }

    @Test
    void upsert_WithNewPerson_CreatesPerson() {
        String personId = "PERSON-" + UUID.randomUUID();
        Instant updatedAt = Instant.now();
        RegisterPersonRequested event = new RegisterPersonRequested(
                personId, personId, "John", "Doe", "1990-01-15", "Male", "White", "555-0100"
        );

        repository.upsert(event, updatedAt);

        Optional<PersonProjectionEntity> result = repository.findByPersonId(personId);
        assertThat(result).isPresent();
        PersonProjectionEntity entity = result.get();
        assertThat(entity.personId()).isEqualTo(personId);
        assertThat(entity.firstName()).isEqualTo("John");
        assertThat(entity.lastName()).isEqualTo("Doe");
        assertThat(entity.dateOfBirth()).isEqualTo("1990-01-15");
    }

    @Test
    void upsert_WithExistingPerson_UpdatesPerson() {
        String personId = "PERSON-" + UUID.randomUUID();
        RegisterPersonRequested event1 = new RegisterPersonRequested(
                personId, personId, "John", "Doe", "1990-01-15", "Male", "White", "555-0100"
        );
        repository.upsert(event1, Instant.now());

        RegisterPersonRequested event2 = new RegisterPersonRequested(
                personId, personId, "Jane", "Smith", "1990-01-15", "Female", "White", "555-0200"
        );
        repository.upsert(event2, Instant.now());

        Optional<PersonProjectionEntity> result = repository.findByPersonId(personId);
        assertThat(result).isPresent();
        PersonProjectionEntity entity = result.get();
        assertThat(entity.firstName()).isEqualTo("Jane");
        assertThat(entity.lastName()).isEqualTo("Smith");
    }

    @Test
    void applyUpdate_WithPartialUpdate_PreservesExistingFields() {
        String personId = "PERSON-" + UUID.randomUUID();
        RegisterPersonRequested createEvent = new RegisterPersonRequested(
                personId, personId, "John", "Doe", "1990-01-15", "Male", "White", "555-0100"
        );
        repository.upsert(createEvent, Instant.now());

        UpdatePersonRequested updateEvent = new UpdatePersonRequested(
                personId, null, "Smith", null, null, null, "555-0200"
        );
        repository.applyUpdate(updateEvent, Instant.now());

        Optional<PersonProjectionEntity> result = repository.findByPersonId(personId);
        assertThat(result).isPresent();
        PersonProjectionEntity entity = result.get();
        assertThat(entity.firstName()).isEqualTo("John"); // Preserved
        assertThat(entity.lastName()).isEqualTo("Smith"); // Updated
        assertThat(entity.phoneNumber()).isEqualTo("555-0200"); // Updated
    }

    @Test
    void findAll_WithFilters_ReturnsFilteredResults() {
        String personId1 = "PERSON-" + UUID.randomUUID();
        RegisterPersonRequested event1 = new RegisterPersonRequested(
                personId1, personId1, "John", "Doe", "1990-01-15", "Male", "White", "555-0100"
        );
        repository.upsert(event1, Instant.now());

        String personId2 = "PERSON-" + UUID.randomUUID();
        RegisterPersonRequested event2 = new RegisterPersonRequested(
                personId2, personId2, "Jane", "Smith", "1990-01-15", "Female", "White", "555-0200"
        );
        repository.upsert(event2, Instant.now());

        List<PersonProjectionEntity> doe = repository.findAll("Doe", null, 0, 10);
        assertThat(doe).hasSize(1);
        assertThat(doe.get(0).personId()).isEqualTo(personId1);
    }

    @Test
    void count_WithFilters_ReturnsCorrectCount() {
        String personId1 = "PERSON-" + UUID.randomUUID();
        RegisterPersonRequested event1 = new RegisterPersonRequested(
                personId1, personId1, "John", "Doe", "1990-01-15", "Male", "White", "555-0100"
        );
        repository.upsert(event1, Instant.now());

        String personId2 = "PERSON-" + UUID.randomUUID();
        RegisterPersonRequested event2 = new RegisterPersonRequested(
                personId2, personId2, "Jane", "Smith", "1990-01-15", "Female", "White", "555-0200"
        );
        repository.upsert(event2, Instant.now());

        long totalCount = repository.count(null, null);
        assertThat(totalCount).isEqualTo(2);

        long doeCount = repository.count("Doe", null);
        assertThat(doeCount).isEqualTo(1);
    }
}
