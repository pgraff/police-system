package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.common.events.resourceassignment.AssignResourceRequested;
import com.knowit.policesystem.common.events.resourceassignment.ChangeResourceAssignmentStatusRequested;
import com.knowit.policesystem.common.events.resourceassignment.UnassignResourceRequested;
import com.knowit.policesystem.projection.integration.IntegrationTestBase;
import com.knowit.policesystem.projection.model.ResourceAssignmentProjectionEntity;
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

class ResourceAssignmentProjectionRepositoryTest extends IntegrationTestBase {

    @Autowired
    private ResourceAssignmentProjectionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clean tables between tests
        jdbcTemplate.execute("TRUNCATE resource_assignment_projection RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE resource_assignment_projection RESTART IDENTITY CASCADE");
    }

    @Test
    void upsert_WithNewAssignment_CreatesResourceAssignment() {
        String assignmentId = "ASSIGN-" + UUID.randomUUID();
        String resourceId = "RES-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        Instant updatedAt = Instant.now();
        
        // Create incident and assignment first (required for foreign key)
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        AssignmentProjectionRepository assignmentRepo = new AssignmentProjectionRepository(jdbcTemplate);
        CreateAssignmentRequested assignmentEvent = new CreateAssignmentRequested(
                assignmentId, assignmentId, Instant.now(), "Primary", "Assigned", incidentId, null
        );
        assignmentRepo.upsert(assignmentEvent, Instant.now());
        
        AssignResourceRequested event = new AssignResourceRequested(
                assignmentId, assignmentId, resourceId, "Officer", "Primary", "Active", startTime
        );

        repository.upsert(event, updatedAt);

        Optional<ResourceAssignmentProjectionEntity> result = repository.findByCompositeKey(assignmentId, resourceId, "Officer");
        assertThat(result).isPresent();
        ResourceAssignmentProjectionEntity entity = result.get();
        assertThat(entity.assignmentId()).isEqualTo(assignmentId);
        assertThat(entity.resourceId()).isEqualTo(resourceId);
        assertThat(entity.resourceType()).isEqualTo("Officer");
        assertThat(entity.roleType()).isEqualTo("Primary");
        assertThat(entity.status()).isEqualTo("Active");
        assertThat(entity.id()).isNotNull(); // Auto-generated
    }

    @Test
    void upsert_WithExistingCompositeKey_UpdatesResourceAssignment() {
        String assignmentId = "ASSIGN-" + UUID.randomUUID();
        String resourceId = "RES-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        
        // Create incident and assignment first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        AssignmentProjectionRepository assignmentRepo = new AssignmentProjectionRepository(jdbcTemplate);
        CreateAssignmentRequested assignmentEvent = new CreateAssignmentRequested(
                assignmentId, assignmentId, Instant.now(), "Primary", "Assigned", incidentId, null
        );
        assignmentRepo.upsert(assignmentEvent, Instant.now());
        
        AssignResourceRequested event1 = new AssignResourceRequested(
                assignmentId, assignmentId, resourceId, "Officer", "Primary", "Active", startTime
        );
        repository.upsert(event1, Instant.now());

        // Get the ID from first insert
        Long firstId = repository.findByCompositeKey(assignmentId, resourceId, "Officer").get().id();

        // Upsert again with different values
        AssignResourceRequested event2 = new AssignResourceRequested(
                assignmentId, assignmentId, resourceId, "Officer", "Backup", "Inactive", startTime.plusSeconds(60)
        );
        repository.upsert(event2, Instant.now());

        Optional<ResourceAssignmentProjectionEntity> result = repository.findByCompositeKey(assignmentId, resourceId, "Officer");
        assertThat(result).isPresent();
        ResourceAssignmentProjectionEntity entity = result.get();
        assertThat(entity.id()).isEqualTo(firstId); // Same ID (updated, not inserted)
        assertThat(entity.roleType()).isEqualTo("Backup"); // Updated
        assertThat(entity.status()).isEqualTo("Inactive"); // Updated
    }

    @Test
    void changeStatus_UpdatesStatus() {
        String assignmentId = "ASSIGN-" + UUID.randomUUID();
        String resourceId = "RES-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        
        // Create incident and assignment first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        AssignmentProjectionRepository assignmentRepo = new AssignmentProjectionRepository(jdbcTemplate);
        CreateAssignmentRequested assignmentEvent = new CreateAssignmentRequested(
                assignmentId, assignmentId, Instant.now(), "Primary", "Assigned", incidentId, null
        );
        assignmentRepo.upsert(assignmentEvent, Instant.now());
        
        AssignResourceRequested createEvent = new AssignResourceRequested(
                assignmentId, assignmentId, resourceId, "Officer", "Primary", "Active", startTime
        );
        repository.upsert(createEvent, Instant.now());

        ChangeResourceAssignmentStatusRequested statusEvent = new ChangeResourceAssignmentStatusRequested(
                assignmentId, assignmentId, resourceId, "Completed"
        );
        repository.changeStatus(statusEvent, Instant.now());

        Optional<ResourceAssignmentProjectionEntity> result = repository.findByCompositeKey(assignmentId, resourceId, "Officer");
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo("Completed");
    }

    @Test
    void unassign_SetsEndTime() {
        String assignmentId = "ASSIGN-" + UUID.randomUUID();
        String resourceId = "RES-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        
        // Create incident and assignment first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        AssignmentProjectionRepository assignmentRepo = new AssignmentProjectionRepository(jdbcTemplate);
        CreateAssignmentRequested assignmentEvent = new CreateAssignmentRequested(
                assignmentId, assignmentId, Instant.now(), "Primary", "Assigned", incidentId, null
        );
        assignmentRepo.upsert(assignmentEvent, Instant.now());
        
        AssignResourceRequested createEvent = new AssignResourceRequested(
                assignmentId, assignmentId, resourceId, "Officer", "Primary", "Active", startTime
        );
        repository.upsert(createEvent, Instant.now());

        Instant endTime = Instant.now();
        UnassignResourceRequested unassignEvent = new UnassignResourceRequested(
                assignmentId, assignmentId, resourceId, endTime
        );
        repository.unassign(unassignEvent, Instant.now());

        Optional<ResourceAssignmentProjectionEntity> result = repository.findByCompositeKey(assignmentId, resourceId, "Officer");
        assertThat(result).isPresent();
        assertThat(result.get().endTime()).isNotNull();
    }

    @Test
    void findByAssignmentId_ReturnsAllResourceAssignmentsForAssignment() {
        String assignmentId = "ASSIGN-" + UUID.randomUUID();
        String resourceId1 = "RES-" + UUID.randomUUID();
        String resourceId2 = "RES-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        
        // Create incident and assignment first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        AssignmentProjectionRepository assignmentRepo = new AssignmentProjectionRepository(jdbcTemplate);
        CreateAssignmentRequested assignmentEvent = new CreateAssignmentRequested(
                assignmentId, assignmentId, Instant.now(), "Primary", "Assigned", incidentId, null
        );
        assignmentRepo.upsert(assignmentEvent, Instant.now());
        
        AssignResourceRequested event1 = new AssignResourceRequested(
                assignmentId, assignmentId, resourceId1, "Officer", "Primary", "Active", Instant.now()
        );
        repository.upsert(event1, Instant.now());

        AssignResourceRequested event2 = new AssignResourceRequested(
                assignmentId, assignmentId, resourceId2, "Vehicle", "Primary", "Active", Instant.now()
        );
        repository.upsert(event2, Instant.now());

        List<ResourceAssignmentProjectionEntity> result = repository.findByAssignmentId(assignmentId);
        assertThat(result).hasSize(2);
    }

    @Test
    void findByResourceId_ReturnsAllAssignmentsForResource() {
        String assignmentId1 = "ASSIGN-" + UUID.randomUUID();
        String assignmentId2 = "ASSIGN-" + UUID.randomUUID();
        String resourceId = "RES-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        
        // Create incident and assignments first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        AssignmentProjectionRepository assignmentRepo = new AssignmentProjectionRepository(jdbcTemplate);
        assignmentRepo.upsert(new CreateAssignmentRequested(
                assignmentId1, assignmentId1, Instant.now(), "Primary", "Assigned", incidentId, null
        ), Instant.now());
        assignmentRepo.upsert(new CreateAssignmentRequested(
                assignmentId2, assignmentId2, Instant.now(), "Secondary", "Assigned", incidentId, null
        ), Instant.now());
        
        AssignResourceRequested event1 = new AssignResourceRequested(
                assignmentId1, assignmentId1, resourceId, "Officer", "Primary", "Active", Instant.now()
        );
        repository.upsert(event1, Instant.now());

        AssignResourceRequested event2 = new AssignResourceRequested(
                assignmentId2, assignmentId2, resourceId, "Officer", "Backup", "Active", Instant.now()
        );
        repository.upsert(event2, Instant.now());

        List<ResourceAssignmentProjectionEntity> result = repository.findByResourceId(resourceId, "Officer");
        assertThat(result).hasSize(2);
    }

    @Test
    void findAll_WithFilters_ReturnsFilteredResults() {
        String assignmentId = "ASSIGN-" + UUID.randomUUID();
        String resourceId1 = "RES-" + UUID.randomUUID();
        String resourceId2 = "RES-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        
        // Create incident and assignment first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        AssignmentProjectionRepository assignmentRepo = new AssignmentProjectionRepository(jdbcTemplate);
        CreateAssignmentRequested assignmentEvent = new CreateAssignmentRequested(
                assignmentId, assignmentId, Instant.now(), "Primary", "Assigned", incidentId, null
        );
        assignmentRepo.upsert(assignmentEvent, Instant.now());
        
        AssignResourceRequested event1 = new AssignResourceRequested(
                assignmentId, assignmentId, resourceId1, "Officer", "Primary", "Active", Instant.now()
        );
        repository.upsert(event1, Instant.now());

        AssignResourceRequested event2 = new AssignResourceRequested(
                assignmentId, assignmentId, resourceId2, "Vehicle", "Backup", "Inactive", Instant.now()
        );
        repository.upsert(event2, Instant.now());

        // Test filtering
        List<ResourceAssignmentProjectionEntity> officers = repository.findAll(assignmentId, null, "Officer", null, null, 0, 10);
        assertThat(officers).hasSize(1);
        assertThat(officers.get(0).resourceId()).isEqualTo(resourceId1);

        List<ResourceAssignmentProjectionEntity> active = repository.findAll(assignmentId, null, null, null, "Active", 0, 10);
        assertThat(active).hasSize(1);
        assertThat(active.get(0).resourceId()).isEqualTo(resourceId1);
    }

    @Test
    void count_WithFilters_ReturnsCorrectCount() {
        String assignmentId = "ASSIGN-" + UUID.randomUUID();
        String resourceId1 = "RES-" + UUID.randomUUID();
        String resourceId2 = "RES-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        
        // Create incident and assignment first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        AssignmentProjectionRepository assignmentRepo = new AssignmentProjectionRepository(jdbcTemplate);
        CreateAssignmentRequested assignmentEvent = new CreateAssignmentRequested(
                assignmentId, assignmentId, Instant.now(), "Primary", "Assigned", incidentId, null
        );
        assignmentRepo.upsert(assignmentEvent, Instant.now());
        
        AssignResourceRequested event1 = new AssignResourceRequested(
                assignmentId, assignmentId, resourceId1, "Officer", "Primary", "Active", Instant.now()
        );
        repository.upsert(event1, Instant.now());

        AssignResourceRequested event2 = new AssignResourceRequested(
                assignmentId, assignmentId, resourceId2, "Vehicle", "Backup", "Inactive", Instant.now()
        );
        repository.upsert(event2, Instant.now());

        long totalCount = repository.count(null, null, null, null, null);
        assertThat(totalCount).isEqualTo(2);

        long activeCount = repository.count(null, null, null, null, "Active");
        assertThat(activeCount).isEqualTo(1);
    }
}
