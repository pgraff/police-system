package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.activities.ChangeActivityStatusRequested;
import com.knowit.policesystem.common.events.activities.LinkActivityToIncidentRequested;
import com.knowit.policesystem.common.events.activities.StartActivityRequested;
import com.knowit.policesystem.common.events.activities.UpdateActivityRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.projection.integration.IntegrationTestBase;
import com.knowit.policesystem.projection.model.ActivityProjectionEntity;
import com.knowit.policesystem.projection.model.ActivityStatusHistoryEntry;
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

class ActivityProjectionRepositoryTest extends IntegrationTestBase {

    @Autowired
    private ActivityProjectionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clean tables between tests
        jdbcTemplate.execute("TRUNCATE activity_status_history, activity_projection RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE activity_status_history, activity_projection RESTART IDENTITY CASCADE");
    }

    @Test
    void upsert_WithNewActivity_CreatesActivity() {
        String activityId = "ACT-" + UUID.randomUUID();
        Instant activityTime = Instant.now();
        Instant updatedAt = Instant.now();
        StartActivityRequested event = new StartActivityRequested(
                activityId, activityTime, "Investigation", "Test activity", "InProgress"
        );

        repository.upsert(event, updatedAt);

        Optional<ActivityProjectionEntity> result = repository.findByActivityId(activityId);
        assertThat(result).isPresent();
        ActivityProjectionEntity entity = result.get();
        assertThat(entity.activityId()).isEqualTo(activityId);
        assertThat(entity.activityType()).isEqualTo("Investigation");
        assertThat(entity.description()).isEqualTo("Test activity");
        assertThat(entity.status()).isEqualTo("InProgress");
    }

    @Test
    void applyUpdate_WithPartialUpdate_PreservesExistingFields() {
        String activityId = "ACT-" + UUID.randomUUID();
        Instant activityTime = Instant.now();
        StartActivityRequested createEvent = new StartActivityRequested(
                activityId, activityTime, "Investigation", "Initial description", "InProgress"
        );
        repository.upsert(createEvent, Instant.now());

        UpdateActivityRequested updateEvent = new UpdateActivityRequested(
                activityId, "Updated description"
        );
        repository.applyUpdate(updateEvent, Instant.now());

        Optional<ActivityProjectionEntity> result = repository.findByActivityId(activityId);
        assertThat(result).isPresent();
        ActivityProjectionEntity entity = result.get();
        assertThat(entity.activityType()).isEqualTo("Investigation"); // Preserved
        assertThat(entity.status()).isEqualTo("InProgress"); // Preserved
        assertThat(entity.description()).isEqualTo("Updated description"); // Updated
    }

    @Test
    void changeStatus_WithStatusChange_UpdatesStatusAndAddsHistory() {
        String activityId = "ACT-" + UUID.randomUUID();
        Instant activityTime = Instant.now();
        StartActivityRequested createEvent = new StartActivityRequested(
                activityId, activityTime, "Investigation", "Test activity", "InProgress"
        );
        repository.upsert(createEvent, Instant.now());

        ChangeActivityStatusRequested statusEvent = new ChangeActivityStatusRequested(
                activityId, "Completed"
        );
        boolean changed = repository.changeStatus(statusEvent, Instant.now());

        assertThat(changed).isTrue();
        Optional<ActivityProjectionEntity> result = repository.findByActivityId(activityId);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo("Completed");

        List<ActivityStatusHistoryEntry> history = repository.findHistory(activityId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).status()).isEqualTo("Completed");
    }

    @Test
    void updateCompletedTime_UpdatesCompletedTime() {
        String activityId = "ACT-" + UUID.randomUUID();
        Instant activityTime = Instant.now();
        StartActivityRequested createEvent = new StartActivityRequested(
                activityId, activityTime, "Investigation", "Test activity", "InProgress"
        );
        repository.upsert(createEvent, Instant.now());

        Instant completedTime = Instant.now();
        repository.updateCompletedTime(activityId, completedTime, Instant.now());

        Optional<ActivityProjectionEntity> result = repository.findByActivityId(activityId);
        assertThat(result).isPresent();
        assertThat(result.get().completedTime()).isNotNull();
    }

    @Test
    void linkToIncident_UpdatesIncidentId() {
        String activityId = "ACT-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        Instant activityTime = Instant.now();
        
        // Create incident first (required for foreign key)
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        StartActivityRequested createEvent = new StartActivityRequested(
                activityId, activityTime, "Investigation", "Test activity", "InProgress"
        );
        repository.upsert(createEvent, Instant.now());

        LinkActivityToIncidentRequested linkEvent = new LinkActivityToIncidentRequested(
                activityId, activityId, incidentId
        );
        repository.linkToIncident(linkEvent, Instant.now());

        Optional<ActivityProjectionEntity> result = repository.findByActivityId(activityId);
        assertThat(result).isPresent();
        assertThat(result.get().incidentId()).isEqualTo(incidentId);
    }

    @Test
    void findAll_WithFilters_ReturnsFilteredResults() {
        // Create test data
        String activityId1 = "ACT-" + UUID.randomUUID();
        StartActivityRequested event1 = new StartActivityRequested(
                activityId1, Instant.now(), "Investigation", "Test 1", "InProgress"
        );
        repository.upsert(event1, Instant.now());

        String activityId2 = "ACT-" + UUID.randomUUID();
        StartActivityRequested event2 = new StartActivityRequested(
                activityId2, Instant.now(), "Patrol", "Test 2", "Completed"
        );
        repository.upsert(event2, Instant.now());

        // Test filtering
        List<ActivityProjectionEntity> inProgress = repository.findAll("InProgress", null, 0, 10);
        assertThat(inProgress).hasSize(1);
        assertThat(inProgress.get(0).activityId()).isEqualTo(activityId1);

        List<ActivityProjectionEntity> investigationType = repository.findAll(null, "Investigation", 0, 10);
        assertThat(investigationType).hasSize(1);
        assertThat(investigationType.get(0).activityId()).isEqualTo(activityId1);
    }

    @Test
    void count_WithFilters_ReturnsCorrectCount() {
        // Create test data
        String activityId1 = "ACT-" + UUID.randomUUID();
        StartActivityRequested event1 = new StartActivityRequested(
                activityId1, Instant.now(), "Investigation", "Test 1", "InProgress"
        );
        repository.upsert(event1, Instant.now());

        String activityId2 = "ACT-" + UUID.randomUUID();
        StartActivityRequested event2 = new StartActivityRequested(
                activityId2, Instant.now(), "Patrol", "Test 2", "Completed"
        );
        repository.upsert(event2, Instant.now());

        long totalCount = repository.count(null, null);
        assertThat(totalCount).isEqualTo(2);

        long inProgressCount = repository.count("InProgress", null);
        assertThat(inProgressCount).isEqualTo(1);
    }

    @Test
    void findHistory_ReturnsStatusHistory() {
        String activityId = "ACT-" + UUID.randomUUID();
        Instant activityTime = Instant.now();
        StartActivityRequested createEvent = new StartActivityRequested(
                activityId, activityTime, "Investigation", "Test activity", "InProgress"
        );
        repository.upsert(createEvent, Instant.now());

        repository.changeStatus(new ChangeActivityStatusRequested(activityId, "Active"), Instant.now());
        repository.changeStatus(new ChangeActivityStatusRequested(activityId, "Completed"), Instant.now());

        List<ActivityStatusHistoryEntry> history = repository.findHistory(activityId);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).status()).isEqualTo("Active");
        assertThat(history.get(1).status()).isEqualTo("Completed");
    }
}
