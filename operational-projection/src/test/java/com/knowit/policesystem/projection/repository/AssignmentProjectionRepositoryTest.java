package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.assignments.ChangeAssignmentStatusRequested;
import com.knowit.policesystem.common.events.assignments.CompleteAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.LinkAssignmentToDispatchRequested;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.dispatches.CreateDispatchRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.projection.integration.IntegrationTestBase;
import com.knowit.policesystem.projection.model.AssignmentProjectionEntity;
import com.knowit.policesystem.projection.model.AssignmentStatusHistoryEntry;
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

class AssignmentProjectionRepositoryTest extends IntegrationTestBase {

    @Autowired
    private AssignmentProjectionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clean tables between tests
        jdbcTemplate.execute("TRUNCATE assignment_status_history, assignment_projection RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE assignment_status_history, assignment_projection RESTART IDENTITY CASCADE");
    }

    @Test
    void upsert_WithNewAssignment_CreatesAssignment() {
        String assignmentId = "ASSIGN-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        Instant assignedTime = Instant.now();
        Instant updatedAt = Instant.now();
        
        // Create incident first (required for foreign key)
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        CreateAssignmentRequested event = new CreateAssignmentRequested(
                assignmentId, assignmentId, assignedTime, "Primary", "Assigned", incidentId, null
        );

        repository.upsert(event, updatedAt);

        Optional<AssignmentProjectionEntity> result = repository.findByAssignmentId(assignmentId);
        assertThat(result).isPresent();
        AssignmentProjectionEntity entity = result.get();
        assertThat(entity.assignmentId()).isEqualTo(assignmentId);
        assertThat(entity.assignmentType()).isEqualTo("Primary");
        assertThat(entity.status()).isEqualTo("Assigned");
        assertThat(entity.incidentId()).isEqualTo(incidentId);
    }

    @Test
    void changeStatus_WithStatusChange_UpdatesStatusAndAddsHistory() {
        String assignmentId = "ASSIGN-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        Instant assignedTime = Instant.now();
        
        // Create incident first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        CreateAssignmentRequested createEvent = new CreateAssignmentRequested(
                assignmentId, assignmentId, assignedTime, "Primary", "Assigned", incidentId, null
        );
        repository.upsert(createEvent, Instant.now());

        ChangeAssignmentStatusRequested statusEvent = new ChangeAssignmentStatusRequested(
                assignmentId, "Active"
        );
        boolean changed = repository.changeStatus(statusEvent, Instant.now());

        assertThat(changed).isTrue();
        Optional<AssignmentProjectionEntity> result = repository.findByAssignmentId(assignmentId);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo("Active");

        List<AssignmentStatusHistoryEntry> history = repository.findHistory(assignmentId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).status()).isEqualTo("Active");
    }

    @Test
    void complete_UpdatesStatusAndCompletedTime() {
        String assignmentId = "ASSIGN-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        Instant assignedTime = Instant.now();
        
        // Create incident first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        CreateAssignmentRequested createEvent = new CreateAssignmentRequested(
                assignmentId, assignmentId, assignedTime, "Primary", "Assigned", incidentId, null
        );
        repository.upsert(createEvent, Instant.now());

        Instant completedTime = Instant.now();
        CompleteAssignmentRequested completeEvent = new CompleteAssignmentRequested(
                assignmentId, completedTime
        );
        repository.complete(completeEvent, Instant.now());

        Optional<AssignmentProjectionEntity> result = repository.findByAssignmentId(assignmentId);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo("Completed");
        assertThat(result.get().completedTime()).isNotNull();

        List<AssignmentStatusHistoryEntry> history = repository.findHistory(assignmentId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).status()).isEqualTo("Completed");
    }

    @Test
    void linkDispatch_UpdatesDispatchId() {
        String assignmentId = "ASSIGN-" + UUID.randomUUID();
        String dispatchId = "DISP-" + UUID.randomUUID();
        String callId = "CALL-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        Instant assignedTime = Instant.now();
        
        // Create incident first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        // Create call first
        CallProjectionRepository callRepo = new CallProjectionRepository(jdbcTemplate);
        ReceiveCallRequested callEvent = new ReceiveCallRequested(
                callId, "CALL-001", "High", "Received",
                Instant.now(), "Test call", "Emergency"
        );
        callRepo.upsert(callEvent, Instant.now());
        
        // Create dispatch first
        DispatchProjectionRepository dispatchRepo = new DispatchProjectionRepository(jdbcTemplate);
        CreateDispatchRequested dispatchEvent = new CreateDispatchRequested(
                dispatchId, Instant.now(), "Initial", "Dispatched"
        );
        dispatchRepo.upsert(dispatchEvent, Instant.now());
        
        CreateAssignmentRequested createEvent = new CreateAssignmentRequested(
                assignmentId, assignmentId, assignedTime, "Primary", "Assigned", incidentId, null
        );
        repository.upsert(createEvent, Instant.now());

        LinkAssignmentToDispatchRequested linkEvent = new LinkAssignmentToDispatchRequested(
                assignmentId, assignmentId, dispatchId
        );
        repository.linkDispatch(linkEvent, Instant.now());

        Optional<AssignmentProjectionEntity> result = repository.findByAssignmentId(assignmentId);
        assertThat(result).isPresent();
        assertThat(result.get().dispatchId()).isEqualTo(dispatchId);
    }

    @Test
    void findAll_WithFilters_ReturnsFilteredResults() {
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
        
        // Create test data
        String assignmentId1 = "ASSIGN-" + UUID.randomUUID();
        CreateAssignmentRequested event1 = new CreateAssignmentRequested(
                assignmentId1, assignmentId1, Instant.now(), "Primary", "Assigned", incidentId1, null
        );
        repository.upsert(event1, Instant.now());

        String assignmentId2 = "ASSIGN-" + UUID.randomUUID();
        CreateAssignmentRequested event2 = new CreateAssignmentRequested(
                assignmentId2, assignmentId2, Instant.now(), "Secondary", "Active", incidentId2, null
        );
        repository.upsert(event2, Instant.now());

        // Test filtering
        List<AssignmentProjectionEntity> assignedStatus = repository.findAll("Assigned", null, null, null, null, 0, 10);
        assertThat(assignedStatus).hasSize(1);
        assertThat(assignedStatus.get(0).assignmentId()).isEqualTo(assignmentId1);

        List<AssignmentProjectionEntity> byIncident = repository.findAll(null, null, null, incidentId1, null, 0, 10);
        assertThat(byIncident).hasSize(1);
        assertThat(byIncident.get(0).assignmentId()).isEqualTo(assignmentId1);
    }

    @Test
    void count_WithFilters_ReturnsCorrectCount() {
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
        
        // Create test data
        String assignmentId1 = "ASSIGN-" + UUID.randomUUID();
        CreateAssignmentRequested event1 = new CreateAssignmentRequested(
                assignmentId1, assignmentId1, Instant.now(), "Primary", "Assigned", incidentId1, null
        );
        repository.upsert(event1, Instant.now());

        String assignmentId2 = "ASSIGN-" + UUID.randomUUID();
        CreateAssignmentRequested event2 = new CreateAssignmentRequested(
                assignmentId2, assignmentId2, Instant.now(), "Secondary", "Active", incidentId2, null
        );
        repository.upsert(event2, Instant.now());

        long totalCount = repository.count(null, null, null, null, null);
        assertThat(totalCount).isEqualTo(2);

        long assignedCount = repository.count("Assigned", null, null, null, null);
        assertThat(assignedCount).isEqualTo(1);
    }

    @Test
    void findHistory_ReturnsStatusHistory() {
        String assignmentId = "ASSIGN-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        Instant assignedTime = Instant.now();
        
        // Create incident first
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        CreateAssignmentRequested createEvent = new CreateAssignmentRequested(
                assignmentId, assignmentId, assignedTime, "Primary", "Assigned", incidentId, null
        );
        repository.upsert(createEvent, Instant.now());

        repository.changeStatus(new ChangeAssignmentStatusRequested(assignmentId, "Active"), Instant.now());
        repository.changeStatus(new ChangeAssignmentStatusRequested(assignmentId, "Completed"), Instant.now());

        List<AssignmentStatusHistoryEntry> history = repository.findHistory(assignmentId);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).status()).isEqualTo("Active");
        assertThat(history.get(1).status()).isEqualTo("Completed");
    }
}
