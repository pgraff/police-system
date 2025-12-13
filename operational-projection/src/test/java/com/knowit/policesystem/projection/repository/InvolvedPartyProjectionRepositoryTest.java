package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.activities.StartActivityRequested;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.involvedparty.EndPartyInvolvementRequested;
import com.knowit.policesystem.common.events.involvedparty.InvolvePartyRequested;
import com.knowit.policesystem.common.events.involvedparty.UpdatePartyInvolvementRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.projection.integration.IntegrationTestBase;
import com.knowit.policesystem.projection.model.InvolvedPartyProjectionEntity;
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

class InvolvedPartyProjectionRepositoryTest extends IntegrationTestBase {

    @Autowired
    private InvolvedPartyProjectionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clean tables between tests
        jdbcTemplate.execute("TRUNCATE involved_party_projection RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE involved_party_projection RESTART IDENTITY CASCADE");
    }

    @Test
    void upsert_WithNewInvolvement_CreatesInvolvement() {
        String involvementId = "INV-" + UUID.randomUUID();
        String personId = "PERSON-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        Instant updatedAt = Instant.now();
        
        // Create incident first (required for foreign key)
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        InvolvePartyRequested event = new InvolvePartyRequested(
                involvementId, involvementId, personId, incidentId, null, null,
                "Victim", "Test involvement", startTime
        );

        repository.upsert(event, updatedAt);

        Optional<InvolvedPartyProjectionEntity> result = repository.findByInvolvementId(involvementId);
        assertThat(result).isPresent();
        InvolvedPartyProjectionEntity entity = result.get();
        assertThat(entity.involvementId()).isEqualTo(involvementId);
        assertThat(entity.personId()).isEqualTo(personId);
        assertThat(entity.partyRoleType()).isEqualTo("Victim");
        assertThat(entity.incidentId()).isEqualTo(incidentId);
        assertThat(entity.callId()).isNull();
        assertThat(entity.activityId()).isNull();
    }

    @Test
    void upsert_WithCall_CreatesInvolvementLinkedToCall() {
        String involvementId = "INV-" + UUID.randomUUID();
        String personId = "PERSON-" + UUID.randomUUID();
        String callId = "CALL-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        
        // Create call first (required for foreign key)
        CallProjectionRepository callRepo = new CallProjectionRepository(jdbcTemplate);
        ReceiveCallRequested callEvent = new ReceiveCallRequested(
                callId, "CALL-001", "High", "Received",
                Instant.now(), "Test call", "Emergency"
        );
        callRepo.upsert(callEvent, Instant.now());
        
        InvolvePartyRequested event = new InvolvePartyRequested(
                involvementId, involvementId, personId, null, callId, null,
                "Witness", "Test involvement", startTime
        );

        repository.upsert(event, Instant.now());

        Optional<InvolvedPartyProjectionEntity> result = repository.findByInvolvementId(involvementId);
        assertThat(result).isPresent();
        InvolvedPartyProjectionEntity entity = result.get();
        assertThat(entity.callId()).isEqualTo(callId);
        assertThat(entity.incidentId()).isNull();
        assertThat(entity.activityId()).isNull();
    }

    @Test
    void upsert_WithActivity_CreatesInvolvementLinkedToActivity() {
        String involvementId = "INV-" + UUID.randomUUID();
        String personId = "PERSON-" + UUID.randomUUID();
        String activityId = "ACT-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        
        // Create activity first (required for foreign key)
        ActivityProjectionRepository activityRepo = new ActivityProjectionRepository(jdbcTemplate);
        StartActivityRequested activityEvent = new StartActivityRequested(
                activityId, Instant.now(), "Investigation", "Test activity", "InProgress"
        );
        activityRepo.upsert(activityEvent, Instant.now());
        
        InvolvePartyRequested event = new InvolvePartyRequested(
                involvementId, involvementId, personId, null, null, activityId,
                "Suspect", "Test involvement", startTime
        );

        repository.upsert(event, Instant.now());

        Optional<InvolvedPartyProjectionEntity> result = repository.findByInvolvementId(involvementId);
        assertThat(result).isPresent();
        InvolvedPartyProjectionEntity entity = result.get();
        assertThat(entity.activityId()).isEqualTo(activityId);
        assertThat(entity.incidentId()).isNull();
        assertThat(entity.callId()).isNull();
    }

    @Test
    void applyUpdate_WithPartialUpdate_PreservesExistingFields() {
        String involvementId = "INV-" + UUID.randomUUID();
        String personId = "PERSON-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        
        // Create incident first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        InvolvePartyRequested createEvent = new InvolvePartyRequested(
                involvementId, involvementId, personId, incidentId, null, null,
                "Victim", "Initial description", startTime
        );
        repository.upsert(createEvent, Instant.now());

        UpdatePartyInvolvementRequested updateEvent = new UpdatePartyInvolvementRequested(
                involvementId, involvementId, "Witness", "Updated description"
        );
        repository.applyUpdate(updateEvent, Instant.now());

        Optional<InvolvedPartyProjectionEntity> result = repository.findByInvolvementId(involvementId);
        assertThat(result).isPresent();
        InvolvedPartyProjectionEntity entity = result.get();
        assertThat(entity.personId()).isEqualTo(personId); // Preserved
        assertThat(entity.incidentId()).isEqualTo(incidentId); // Preserved
        assertThat(entity.partyRoleType()).isEqualTo("Witness"); // Updated
        assertThat(entity.description()).isEqualTo("Updated description"); // Updated
    }

    @Test
    void endInvolvement_SetsEndTime() {
        String involvementId = "INV-" + UUID.randomUUID();
        String personId = "PERSON-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        
        // Create incident first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        InvolvePartyRequested createEvent = new InvolvePartyRequested(
                involvementId, involvementId, personId, incidentId, null, null,
                "Victim", "Test involvement", startTime
        );
        repository.upsert(createEvent, Instant.now());

        Instant endTime = Instant.now();
        EndPartyInvolvementRequested endEvent = new EndPartyInvolvementRequested(
                involvementId, involvementId, endTime
        );
        repository.endInvolvement(endEvent, Instant.now());

        Optional<InvolvedPartyProjectionEntity> result = repository.findByInvolvementId(involvementId);
        assertThat(result).isPresent();
        assertThat(result.get().involvementEndTime()).isNotNull();
    }

    @Test
    void findByPersonId_ReturnsAllInvolvementsForPerson() {
        String personId = "PERSON-" + UUID.randomUUID();
        String incidentId1 = "INC-" + UUID.randomUUID();
        String incidentId2 = "INC-" + UUID.randomUUID();
        
        // Create incidents first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        incidentRepo.upsert(new ReportIncidentRequested(
                incidentId1, "2024-001", "High", "Reported",
                Instant.now(), "Test 1", "Traffic"
        ), Instant.now());
        incidentRepo.upsert(new ReportIncidentRequested(
                incidentId2, "2024-002", "Medium", "Reported",
                Instant.now(), "Test 2", "Emergency"
        ), Instant.now());
        
        String involvementId1 = "INV-" + UUID.randomUUID();
        InvolvePartyRequested event1 = new InvolvePartyRequested(
                involvementId1, involvementId1, personId, incidentId1, null, null,
                "Victim", "Test 1", Instant.now()
        );
        repository.upsert(event1, Instant.now());

        String involvementId2 = "INV-" + UUID.randomUUID();
        InvolvePartyRequested event2 = new InvolvePartyRequested(
                involvementId2, involvementId2, personId, incidentId2, null, null,
                "Witness", "Test 2", Instant.now()
        );
        repository.upsert(event2, Instant.now());

        List<InvolvedPartyProjectionEntity> result = repository.findByPersonId(personId);
        assertThat(result).hasSize(2);
    }

    @Test
    void findByIncidentId_ReturnsAllInvolvementsForIncident() {
        String incidentId = "INC-" + UUID.randomUUID();
        String personId1 = "PERSON-" + UUID.randomUUID();
        String personId2 = "PERSON-" + UUID.randomUUID();
        
        // Create incident first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        incidentRepo.upsert(new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        ), Instant.now());
        
        String involvementId1 = "INV-" + UUID.randomUUID();
        InvolvePartyRequested event1 = new InvolvePartyRequested(
                involvementId1, involvementId1, personId1, incidentId, null, null,
                "Victim", "Test 1", Instant.now()
        );
        repository.upsert(event1, Instant.now());

        String involvementId2 = "INV-" + UUID.randomUUID();
        InvolvePartyRequested event2 = new InvolvePartyRequested(
                involvementId2, involvementId2, personId2, incidentId, null, null,
                "Witness", "Test 2", Instant.now()
        );
        repository.upsert(event2, Instant.now());

        List<InvolvedPartyProjectionEntity> result = repository.findByIncidentId(incidentId);
        assertThat(result).hasSize(2);
    }

    @Test
    void findAll_WithFilters_ReturnsFilteredResults() {
        String personId = "PERSON-" + UUID.randomUUID();
        String incidentId1 = "INC-" + UUID.randomUUID();
        String incidentId2 = "INC-" + UUID.randomUUID();
        
        // Create incidents first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        incidentRepo.upsert(new ReportIncidentRequested(
                incidentId1, "2024-001", "High", "Reported",
                Instant.now(), "Test 1", "Traffic"
        ), Instant.now());
        incidentRepo.upsert(new ReportIncidentRequested(
                incidentId2, "2024-002", "Medium", "Reported",
                Instant.now(), "Test 2", "Emergency"
        ), Instant.now());
        
        String involvementId1 = "INV-" + UUID.randomUUID();
        InvolvePartyRequested event1 = new InvolvePartyRequested(
                involvementId1, involvementId1, personId, incidentId1, null, null,
                "Victim", "Test 1", Instant.now()
        );
        repository.upsert(event1, Instant.now());

        String involvementId2 = "INV-" + UUID.randomUUID();
        InvolvePartyRequested event2 = new InvolvePartyRequested(
                involvementId2, involvementId2, personId, incidentId2, null, null,
                "Witness", "Test 2", Instant.now()
        );
        repository.upsert(event2, Instant.now());

        // Test filtering
        List<InvolvedPartyProjectionEntity> victims = repository.findAll(personId, "Victim", null, null, null, 0, 10);
        assertThat(victims).hasSize(1);
        assertThat(victims.get(0).involvementId()).isEqualTo(involvementId1);

        List<InvolvedPartyProjectionEntity> byIncident = repository.findAll(null, null, incidentId1, null, null, 0, 10);
        assertThat(byIncident).hasSize(1);
        assertThat(byIncident.get(0).involvementId()).isEqualTo(involvementId1);
    }

    @Test
    void count_WithFilters_ReturnsCorrectCount() {
        String personId = "PERSON-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        
        // Create incident first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        incidentRepo.upsert(new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        ), Instant.now());
        
        String involvementId1 = "INV-" + UUID.randomUUID();
        InvolvePartyRequested event1 = new InvolvePartyRequested(
                involvementId1, involvementId1, personId, incidentId, null, null,
                "Victim", "Test 1", Instant.now()
        );
        repository.upsert(event1, Instant.now());

        String involvementId2 = "INV-" + UUID.randomUUID();
        InvolvePartyRequested event2 = new InvolvePartyRequested(
                involvementId2, involvementId2, personId, incidentId, null, null,
                "Witness", "Test 2", Instant.now()
        );
        repository.upsert(event2, Instant.now());

        long totalCount = repository.count(null, null, null, null, null);
        assertThat(totalCount).isEqualTo(2);

        long victimCount = repository.count(null, "Victim", null, null, null);
        assertThat(victimCount).isEqualTo(1);
    }
}
