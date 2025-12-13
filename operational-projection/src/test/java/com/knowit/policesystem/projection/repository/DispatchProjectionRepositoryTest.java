package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.calls.LinkCallToDispatchRequested;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.dispatches.ChangeDispatchStatusRequested;
import com.knowit.policesystem.common.events.dispatches.CreateDispatchRequested;
import com.knowit.policesystem.projection.integration.IntegrationTestBase;
import com.knowit.policesystem.projection.model.DispatchProjectionEntity;
import com.knowit.policesystem.projection.model.DispatchStatusHistoryEntry;
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

class DispatchProjectionRepositoryTest extends IntegrationTestBase {

    @Autowired
    private DispatchProjectionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clean tables between tests
        jdbcTemplate.execute("TRUNCATE dispatch_status_history, dispatch_projection RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE dispatch_status_history, dispatch_projection RESTART IDENTITY CASCADE");
    }

    @Test
    void upsert_WithNewDispatch_CreatesDispatch() {
        String dispatchId = "DISP-" + UUID.randomUUID();
        Instant dispatchTime = Instant.now();
        Instant updatedAt = Instant.now();
        CreateDispatchRequested event = new CreateDispatchRequested(
                dispatchId, dispatchTime, "Initial", "Dispatched"
        );

        repository.upsert(event, updatedAt);

        Optional<DispatchProjectionEntity> result = repository.findByDispatchId(dispatchId);
        assertThat(result).isPresent();
        DispatchProjectionEntity entity = result.get();
        assertThat(entity.dispatchId()).isEqualTo(dispatchId);
        assertThat(entity.dispatchType()).isEqualTo("Initial");
        assertThat(entity.status()).isEqualTo("Dispatched");
    }

    @Test
    void upsert_WithExistingDispatch_UpdatesDispatch() {
        String dispatchId = "DISP-" + UUID.randomUUID();
        Instant dispatchTime = Instant.now();
        CreateDispatchRequested event1 = new CreateDispatchRequested(
                dispatchId, dispatchTime, "Initial", "Dispatched"
        );
        repository.upsert(event1, Instant.now());

        CreateDispatchRequested event2 = new CreateDispatchRequested(
                dispatchId, dispatchTime, "FollowUp", "Active"
        );
        repository.upsert(event2, Instant.now());

        Optional<DispatchProjectionEntity> result = repository.findByDispatchId(dispatchId);
        assertThat(result).isPresent();
        DispatchProjectionEntity entity = result.get();
        assertThat(entity.dispatchType()).isEqualTo("FollowUp");
        assertThat(entity.status()).isEqualTo("Active");
    }

    @Test
    void changeStatus_WithStatusChange_UpdatesStatusAndAddsHistory() {
        String dispatchId = "DISP-" + UUID.randomUUID();
        Instant dispatchTime = Instant.now();
        CreateDispatchRequested createEvent = new CreateDispatchRequested(
                dispatchId, dispatchTime, "Initial", "Dispatched"
        );
        repository.upsert(createEvent, Instant.now());

        ChangeDispatchStatusRequested statusEvent = new ChangeDispatchStatusRequested(
                dispatchId, "Active"
        );
        boolean changed = repository.changeStatus(statusEvent, Instant.now());

        assertThat(changed).isTrue();
        Optional<DispatchProjectionEntity> result = repository.findByDispatchId(dispatchId);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo("Active");

        List<DispatchStatusHistoryEntry> history = repository.findHistory(dispatchId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).status()).isEqualTo("Active");
    }

    @Test
    void changeStatus_WithSameStatus_DoesNotAddHistory() {
        String dispatchId = "DISP-" + UUID.randomUUID();
        Instant dispatchTime = Instant.now();
        CreateDispatchRequested createEvent = new CreateDispatchRequested(
                dispatchId, dispatchTime, "Initial", "Dispatched"
        );
        repository.upsert(createEvent, Instant.now());

        ChangeDispatchStatusRequested statusEvent1 = new ChangeDispatchStatusRequested(
                dispatchId, "Active"
        );
        repository.changeStatus(statusEvent1, Instant.now());

        ChangeDispatchStatusRequested statusEvent2 = new ChangeDispatchStatusRequested(
                dispatchId, "Active"
        );
        boolean changed = repository.changeStatus(statusEvent2, Instant.now());

        assertThat(changed).isFalse();
        List<DispatchStatusHistoryEntry> history = repository.findHistory(dispatchId);
        assertThat(history).hasSize(1); // Only one entry
    }

    @Test
    void linkToCall_UpdatesCallId() {
        String dispatchId = "DISP-" + UUID.randomUUID();
        String callId = "CALL-" + UUID.randomUUID();
        Instant dispatchTime = Instant.now();
        
        // Create call first (required for foreign key)
        CallProjectionRepository callRepo = new CallProjectionRepository(jdbcTemplate);
        ReceiveCallRequested callEvent = new ReceiveCallRequested(
                callId, "CALL-001", "High", "Received",
                Instant.now(), "Test call", "Emergency"
        );
        callRepo.upsert(callEvent, Instant.now());
        
        CreateDispatchRequested createEvent = new CreateDispatchRequested(
                dispatchId, dispatchTime, "Initial", "Dispatched"
        );
        repository.upsert(createEvent, Instant.now());

        LinkCallToDispatchRequested linkEvent = new LinkCallToDispatchRequested(
                callId, callId, dispatchId
        );
        repository.linkToCall(linkEvent, Instant.now());

        Optional<DispatchProjectionEntity> result = repository.findByDispatchId(dispatchId);
        assertThat(result).isPresent();
        assertThat(result.get().callId()).isEqualTo(callId);
    }

    @Test
    void findAll_WithFilters_ReturnsFilteredResults() {
        // Create test data
        String dispatchId1 = "DISP-" + UUID.randomUUID();
        CreateDispatchRequested event1 = new CreateDispatchRequested(
                dispatchId1, Instant.now(), "Initial", "Dispatched"
        );
        repository.upsert(event1, Instant.now());

        String dispatchId2 = "DISP-" + UUID.randomUUID();
        CreateDispatchRequested event2 = new CreateDispatchRequested(
                dispatchId2, Instant.now(), "FollowUp", "Active"
        );
        repository.upsert(event2, Instant.now());

        // Test filtering
        List<DispatchProjectionEntity> dispatchedStatus = repository.findAll("Dispatched", null, 0, 10);
        assertThat(dispatchedStatus).hasSize(1);
        assertThat(dispatchedStatus.get(0).dispatchId()).isEqualTo(dispatchId1);

        List<DispatchProjectionEntity> initialType = repository.findAll(null, "Initial", 0, 10);
        assertThat(initialType).hasSize(1);
        assertThat(initialType.get(0).dispatchId()).isEqualTo(dispatchId1);
    }

    @Test
    void count_WithFilters_ReturnsCorrectCount() {
        // Create test data
        String dispatchId1 = "DISP-" + UUID.randomUUID();
        CreateDispatchRequested event1 = new CreateDispatchRequested(
                dispatchId1, Instant.now(), "Initial", "Dispatched"
        );
        repository.upsert(event1, Instant.now());

        String dispatchId2 = "DISP-" + UUID.randomUUID();
        CreateDispatchRequested event2 = new CreateDispatchRequested(
                dispatchId2, Instant.now(), "FollowUp", "Active"
        );
        repository.upsert(event2, Instant.now());

        long totalCount = repository.count(null, null);
        assertThat(totalCount).isEqualTo(2);

        long dispatchedCount = repository.count("Dispatched", null);
        assertThat(dispatchedCount).isEqualTo(1);
    }

    @Test
    void findHistory_ReturnsStatusHistory() {
        String dispatchId = "DISP-" + UUID.randomUUID();
        Instant dispatchTime = Instant.now();
        CreateDispatchRequested createEvent = new CreateDispatchRequested(
                dispatchId, dispatchTime, "Initial", "Dispatched"
        );
        repository.upsert(createEvent, Instant.now());

        repository.changeStatus(new ChangeDispatchStatusRequested(dispatchId, "Active"), Instant.now());
        repository.changeStatus(new ChangeDispatchStatusRequested(dispatchId, "Completed"), Instant.now());

        List<DispatchStatusHistoryEntry> history = repository.findHistory(dispatchId);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).status()).isEqualTo("Active");
        assertThat(history.get(1).status()).isEqualTo("Completed");
    }
}
