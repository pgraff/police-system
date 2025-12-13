package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.calls.ChangeCallStatusRequested;
import com.knowit.policesystem.common.events.calls.LinkCallToIncidentRequested;
import com.knowit.policesystem.common.events.calls.LinkCallToDispatchRequested;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.calls.UpdateCallRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.projection.integration.IntegrationTestBase;
import com.knowit.policesystem.projection.model.CallProjectionEntity;
import com.knowit.policesystem.projection.model.CallStatusHistoryEntry;
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

class CallProjectionRepositoryTest extends IntegrationTestBase {

    @Autowired
    private CallProjectionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clean tables between tests
        jdbcTemplate.execute("TRUNCATE call_status_history, call_projection RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE call_status_history, call_projection RESTART IDENTITY CASCADE");
    }

    @Test
    void upsert_WithNewCall_CreatesCall() {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        Instant updatedAt = Instant.now();
        ReceiveCallRequested event = new ReceiveCallRequested(
                callId, "CALL-001", "High", "Received",
                receivedTime, "Test call", "Emergency"
        );

        repository.upsert(event, updatedAt);

        Optional<CallProjectionEntity> result = repository.findByCallId(callId);
        assertThat(result).isPresent();
        CallProjectionEntity entity = result.get();
        assertThat(entity.callId()).isEqualTo(callId);
        assertThat(entity.callNumber()).isEqualTo("CALL-001");
        assertThat(entity.priority()).isEqualTo("High");
        assertThat(entity.status()).isEqualTo("Received");
        assertThat(entity.description()).isEqualTo("Test call");
        assertThat(entity.callType()).isEqualTo("Emergency");
    }

    @Test
    void applyUpdate_WithPartialUpdate_PreservesExistingFields() {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        ReceiveCallRequested createEvent = new ReceiveCallRequested(
                callId, "CALL-001", "High", "Received",
                receivedTime, "Initial description", "Emergency"
        );
        repository.upsert(createEvent, Instant.now());

        UpdateCallRequested updateEvent = new UpdateCallRequested(
                callId, "Medium", "Updated description", "NonEmergency"
        );
        repository.applyUpdate(updateEvent, Instant.now());

        Optional<CallProjectionEntity> result = repository.findByCallId(callId);
        assertThat(result).isPresent();
        CallProjectionEntity entity = result.get();
        assertThat(entity.callNumber()).isEqualTo("CALL-001"); // Preserved
        assertThat(entity.priority()).isEqualTo("Medium"); // Updated
        assertThat(entity.status()).isEqualTo("Received"); // Preserved
        assertThat(entity.description()).isEqualTo("Updated description"); // Updated
        assertThat(entity.callType()).isEqualTo("NonEmergency"); // Updated
    }

    @Test
    void changeStatus_WithStatusChange_UpdatesStatusAndAddsHistory() {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        ReceiveCallRequested createEvent = new ReceiveCallRequested(
                callId, "CALL-001", "High", "Received",
                receivedTime, "Test call", "Emergency"
        );
        repository.upsert(createEvent, Instant.now());

        ChangeCallStatusRequested statusEvent = new ChangeCallStatusRequested(
                callId, "Dispatched"
        );
        boolean changed = repository.changeStatus(statusEvent, Instant.now());

        assertThat(changed).isTrue();
        Optional<CallProjectionEntity> result = repository.findByCallId(callId);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo("Dispatched");

        List<CallStatusHistoryEntry> history = repository.findHistory(callId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).status()).isEqualTo("Dispatched");
    }

    @Test
    void linkToIncident_UpdatesIncidentId() {
        String callId = "CALL-" + UUID.randomUUID();
        String incidentId = "INC-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        
        // Create incident first (required for foreign key)
        IncidentProjectionRepository incidentRepo = new IncidentProjectionRepository(jdbcTemplate);
        ReportIncidentRequested incidentEvent = new ReportIncidentRequested(
                incidentId, "2024-001", "High", "Reported",
                Instant.now(), "Test incident", "Traffic"
        );
        incidentRepo.upsert(incidentEvent, Instant.now());
        
        ReceiveCallRequested createEvent = new ReceiveCallRequested(
                callId, "CALL-001", "High", "Received",
                receivedTime, "Test call", "Emergency"
        );
        repository.upsert(createEvent, Instant.now());

        LinkCallToIncidentRequested linkEvent = new LinkCallToIncidentRequested(
                callId, callId, incidentId
        );
        repository.linkToIncident(linkEvent, Instant.now());

        Optional<CallProjectionEntity> result = repository.findByCallId(callId);
        assertThat(result).isPresent();
        assertThat(result.get().incidentId()).isEqualTo(incidentId);
    }

    @Test
    void linkToDispatch_IsNoOp() {
        String callId = "CALL-" + UUID.randomUUID();
        String dispatchId = "DISP-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        ReceiveCallRequested createEvent = new ReceiveCallRequested(
                callId, "CALL-001", "High", "Received",
                receivedTime, "Test call", "Emergency"
        );
        repository.upsert(createEvent, Instant.now());

        LinkCallToDispatchRequested linkEvent = new LinkCallToDispatchRequested(
                callId, callId, dispatchId
        );
        // This should be a no-op - the link is maintained in dispatch_projection
        repository.linkToDispatch(linkEvent, Instant.now());

        // Verify call still exists and wasn't affected
        Optional<CallProjectionEntity> result = repository.findByCallId(callId);
        assertThat(result).isPresent();
    }

    @Test
    void updateDispatchTime_UpdatesDispatchedTime() {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        ReceiveCallRequested createEvent = new ReceiveCallRequested(
                callId, "CALL-001", "High", "Received",
                receivedTime, "Test call", "Emergency"
        );
        repository.upsert(createEvent, Instant.now());

        Instant dispatchedTime = Instant.now();
        repository.updateDispatchTime(callId, dispatchedTime, Instant.now());

        Optional<CallProjectionEntity> result = repository.findByCallId(callId);
        assertThat(result).isPresent();
        assertThat(result.get().dispatchedTime()).isNotNull();
    }

    @Test
    void updateArrivedTime_UpdatesArrivedTime() {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        ReceiveCallRequested createEvent = new ReceiveCallRequested(
                callId, "CALL-001", "High", "Received",
                receivedTime, "Test call", "Emergency"
        );
        repository.upsert(createEvent, Instant.now());

        Instant arrivedTime = Instant.now();
        repository.updateArrivedTime(callId, arrivedTime, Instant.now());

        Optional<CallProjectionEntity> result = repository.findByCallId(callId);
        assertThat(result).isPresent();
        assertThat(result.get().arrivedTime()).isNotNull();
    }

    @Test
    void updateClearedTime_UpdatesClearedTime() {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        ReceiveCallRequested createEvent = new ReceiveCallRequested(
                callId, "CALL-001", "High", "Received",
                receivedTime, "Test call", "Emergency"
        );
        repository.upsert(createEvent, Instant.now());

        Instant clearedTime = Instant.now();
        repository.updateClearedTime(callId, clearedTime, Instant.now());

        Optional<CallProjectionEntity> result = repository.findByCallId(callId);
        assertThat(result).isPresent();
        assertThat(result.get().clearedTime()).isNotNull();
    }

    @Test
    void findAll_WithFilters_ReturnsFilteredResults() {
        // Create test data
        String callId1 = "CALL-" + UUID.randomUUID();
        ReceiveCallRequested event1 = new ReceiveCallRequested(
                callId1, "CALL-001", "High", "Received",
                Instant.now(), "Test 1", "Emergency"
        );
        repository.upsert(event1, Instant.now());

        String callId2 = "CALL-" + UUID.randomUUID();
        ReceiveCallRequested event2 = new ReceiveCallRequested(
                callId2, "CALL-002", "Medium", "Dispatched",
                Instant.now(), "Test 2", "NonEmergency"
        );
        repository.upsert(event2, Instant.now());

        // Test filtering
        List<CallProjectionEntity> receivedStatus = repository.findAll("Received", "High", null, 0, 10);
        assertThat(receivedStatus).hasSize(1);
        assertThat(receivedStatus.get(0).callId()).isEqualTo(callId1);

        List<CallProjectionEntity> emergencyType = repository.findAll(null, null, "Emergency", 0, 10);
        assertThat(emergencyType).hasSize(1);
        assertThat(emergencyType.get(0).callId()).isEqualTo(callId1);
    }

    @Test
    void count_WithFilters_ReturnsCorrectCount() {
        // Create test data
        String callId1 = "CALL-" + UUID.randomUUID();
        ReceiveCallRequested event1 = new ReceiveCallRequested(
                callId1, "CALL-001", "High", "Received",
                Instant.now(), "Test 1", "Emergency"
        );
        repository.upsert(event1, Instant.now());

        String callId2 = "CALL-" + UUID.randomUUID();
        ReceiveCallRequested event2 = new ReceiveCallRequested(
                callId2, "CALL-002", "Medium", "Dispatched",
                Instant.now(), "Test 2", "NonEmergency"
        );
        repository.upsert(event2, Instant.now());

        long totalCount = repository.count(null, null, null);
        assertThat(totalCount).isEqualTo(2);

        long receivedCount = repository.count("Received", "High", null);
        assertThat(receivedCount).isEqualTo(1);
    }

    @Test
    void findHistory_ReturnsStatusHistory() {
        String callId = "CALL-" + UUID.randomUUID();
        Instant receivedTime = Instant.now();
        ReceiveCallRequested createEvent = new ReceiveCallRequested(
                callId, "CALL-001", "High", "Received",
                receivedTime, "Test call", "Emergency"
        );
        repository.upsert(createEvent, Instant.now());

        repository.changeStatus(new ChangeCallStatusRequested(callId, "Dispatched"), Instant.now());
        repository.changeStatus(new ChangeCallStatusRequested(callId, "Arrived"), Instant.now());

        List<CallStatusHistoryEntry> history = repository.findHistory(callId);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).status()).isEqualTo("Dispatched");
        assertThat(history.get(1).status()).isEqualTo("Arrived");
    }
}
