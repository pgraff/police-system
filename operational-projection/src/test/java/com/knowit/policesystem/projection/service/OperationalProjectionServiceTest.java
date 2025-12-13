package com.knowit.policesystem.projection.service;

import com.knowit.policesystem.common.events.activities.ChangeActivityStatusRequested;
import com.knowit.policesystem.common.events.activities.CompleteActivityRequested;
import com.knowit.policesystem.common.events.activities.LinkActivityToIncidentRequested;
import com.knowit.policesystem.common.events.activities.StartActivityRequested;
import com.knowit.policesystem.common.events.activities.UpdateActivityRequested;
import com.knowit.policesystem.common.events.assignments.ChangeAssignmentStatusRequested;
import com.knowit.policesystem.common.events.assignments.CompleteAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.LinkAssignmentToDispatchRequested;
import com.knowit.policesystem.common.events.calls.ArriveAtCallRequested;
import com.knowit.policesystem.common.events.calls.ChangeCallStatusRequested;
import com.knowit.policesystem.common.events.calls.ClearCallRequested;
import com.knowit.policesystem.common.events.calls.DispatchCallRequested;
import com.knowit.policesystem.common.events.calls.LinkCallToDispatchRequested;
import com.knowit.policesystem.common.events.calls.LinkCallToIncidentRequested;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.calls.UpdateCallRequested;
import com.knowit.policesystem.common.events.dispatches.ChangeDispatchStatusRequested;
import com.knowit.policesystem.common.events.dispatches.CreateDispatchRequested;
import com.knowit.policesystem.common.events.incidents.ArriveAtIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ChangeIncidentStatusRequested;
import com.knowit.policesystem.common.events.incidents.ClearIncidentRequested;
import com.knowit.policesystem.common.events.incidents.DispatchIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.common.events.incidents.UpdateIncidentRequested;
import com.knowit.policesystem.common.events.involvedparty.EndPartyInvolvementRequested;
import com.knowit.policesystem.common.events.involvedparty.InvolvePartyRequested;
import com.knowit.policesystem.common.events.involvedparty.UpdatePartyInvolvementRequested;
import com.knowit.policesystem.common.events.resourceassignment.AssignResourceRequested;
import com.knowit.policesystem.common.events.resourceassignment.ChangeResourceAssignmentStatusRequested;
import com.knowit.policesystem.common.events.resourceassignment.UnassignResourceRequested;
import com.knowit.policesystem.projection.repository.ActivityProjectionRepository;
import com.knowit.policesystem.projection.repository.AssignmentProjectionRepository;
import com.knowit.policesystem.projection.repository.CallProjectionRepository;
import com.knowit.policesystem.projection.repository.DispatchProjectionRepository;
import com.knowit.policesystem.projection.repository.IncidentProjectionRepository;
import com.knowit.policesystem.projection.repository.InvolvedPartyProjectionRepository;
import com.knowit.policesystem.projection.repository.ResourceAssignmentProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalProjectionServiceTest {

    @Mock
    private IncidentProjectionRepository incidentRepository;

    @Mock
    private CallProjectionRepository callRepository;

    @Mock
    private DispatchProjectionRepository dispatchRepository;

    @Mock
    private ActivityProjectionRepository activityRepository;

    @Mock
    private AssignmentProjectionRepository assignmentRepository;

    @Mock
    private InvolvedPartyProjectionRepository involvedPartyRepository;

    @Mock
    private ResourceAssignmentProjectionRepository resourceAssignmentRepository;

    private Clock clock;
    private OperationalProjectionService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), ZoneId.of("UTC"));
        service = new OperationalProjectionService(
                incidentRepository,
                callRepository,
                dispatchRepository,
                activityRepository,
                assignmentRepository,
                involvedPartyRepository,
                resourceAssignmentRepository,
                clock
        );
    }

    // Incident event tests
    @Test
    void handle_ReportIncidentRequested_RoutesToIncidentRepository() {
        ReportIncidentRequested event = new ReportIncidentRequested(
                "INC-001", "2024-001", "High", "Reported", Instant.now(), "Test", "Traffic"
        );

        service.handle(event);

        verify(incidentRepository).upsert(eq(event), any(Instant.class));
    }

    @Test
    void handle_UpdateIncidentRequested_RoutesToIncidentRepository() {
        UpdateIncidentRequested event = new UpdateIncidentRequested("INC-001", "High", "Updated description", "Traffic");

        service.handle(event);

        verify(incidentRepository).applyUpdate(eq(event), any(Instant.class));
    }

    @Test
    void handle_ChangeIncidentStatusRequested_RoutesToIncidentRepository() {
        ChangeIncidentStatusRequested event = new ChangeIncidentStatusRequested("INC-001", "Active");

        service.handle(event);

        verify(incidentRepository).changeStatus(eq(event), any(Instant.class));
    }

    @Test
    void handle_DispatchIncidentRequested_RoutesToIncidentRepository() {
        DispatchIncidentRequested event = new DispatchIncidentRequested("INC-001", Instant.now());

        service.handle(event);

        verify(incidentRepository).updateDispatchTime(eq("INC-001"), any(Instant.class), any(Instant.class));
    }

    @Test
    void handle_ArriveAtIncidentRequested_RoutesToIncidentRepository() {
        ArriveAtIncidentRequested event = new ArriveAtIncidentRequested("INC-001", Instant.now());

        service.handle(event);

        verify(incidentRepository).updateArrivedTime(eq("INC-001"), any(Instant.class), any(Instant.class));
    }

    @Test
    void handle_ClearIncidentRequested_RoutesToIncidentRepository() {
        ClearIncidentRequested event = new ClearIncidentRequested("INC-001", Instant.now());

        service.handle(event);

        verify(incidentRepository).updateClearedTime(eq("INC-001"), any(Instant.class), any(Instant.class));
    }

    // Call event tests
    @Test
    void handle_ReceiveCallRequested_RoutesToCallRepository() {
        ReceiveCallRequested event = new ReceiveCallRequested(
                "CALL-001", "CALL-001", "High", "Received", Instant.now(), "Test", "Emergency"
        );

        service.handle(event);

        verify(callRepository).upsert(eq(event), any(Instant.class));
    }

    @Test
    void handle_UpdateCallRequested_RoutesToCallRepository() {
        UpdateCallRequested event = new UpdateCallRequested("CALL-001", "High", "Updated description", "Emergency");

        service.handle(event);

        verify(callRepository).applyUpdate(eq(event), any(Instant.class));
    }

    @Test
    void handle_ChangeCallStatusRequested_RoutesToCallRepository() {
        ChangeCallStatusRequested event = new ChangeCallStatusRequested("CALL-001", "Active");

        service.handle(event);

        verify(callRepository).changeStatus(eq(event), any(Instant.class));
    }

    @Test
    void handle_DispatchCallRequested_RoutesToCallRepository() {
        DispatchCallRequested event = new DispatchCallRequested("CALL-001", Instant.now());

        service.handle(event);

        verify(callRepository).updateDispatchTime(eq("CALL-001"), any(Instant.class), any(Instant.class));
    }

    @Test
    void handle_ArriveAtCallRequested_RoutesToCallRepository() {
        ArriveAtCallRequested event = new ArriveAtCallRequested("CALL-001", Instant.now());

        service.handle(event);

        verify(callRepository).updateArrivedTime(eq("CALL-001"), any(Instant.class), any(Instant.class));
    }

    @Test
    void handle_ClearCallRequested_RoutesToCallRepository() {
        ClearCallRequested event = new ClearCallRequested("CALL-001", Instant.now());

        service.handle(event);

        verify(callRepository).updateClearedTime(eq("CALL-001"), any(Instant.class), any(Instant.class));
    }

    @Test
    void handle_LinkCallToIncidentRequested_RoutesToCallRepository() {
        LinkCallToIncidentRequested event = new LinkCallToIncidentRequested("CALL-001", "CALL-001", "INC-001");

        service.handle(event);

        verify(callRepository).linkToIncident(eq(event), any(Instant.class));
    }

    @Test
    void handle_LinkCallToDispatchRequested_DoesNotCallRepository() {
        LinkCallToDispatchRequested event = new LinkCallToDispatchRequested("CALL-001", "CALL-001", "DISP-001");

        service.handle(event);

        // This is a no-op (handled by DispatchProjectionRepository)
        verify(callRepository, never()).linkToDispatch(any(), any());
    }

    // Dispatch event tests
    @Test
    void handle_CreateDispatchRequested_RoutesToDispatchRepository() {
        CreateDispatchRequested event = new CreateDispatchRequested("DISP-001", Instant.now(), "Initial", "Dispatched");

        service.handle(event);

        verify(dispatchRepository).upsert(eq(event), any(Instant.class));
    }

    @Test
    void handle_ChangeDispatchStatusRequested_RoutesToDispatchRepository() {
        ChangeDispatchStatusRequested event = new ChangeDispatchStatusRequested("DISP-001", "Active");

        service.handle(event);

        verify(dispatchRepository).changeStatus(eq(event), any(Instant.class));
    }

    // Activity event tests
    @Test
    void handle_StartActivityRequested_RoutesToActivityRepository() {
        StartActivityRequested event = new StartActivityRequested(
                "ACT-001", Instant.now(), "Investigation", "Test activity", "InProgress"
        );

        service.handle(event);

        verify(activityRepository).upsert(eq(event), any(Instant.class));
    }

    @Test
    void handle_UpdateActivityRequested_RoutesToActivityRepository() {
        UpdateActivityRequested event = new UpdateActivityRequested("ACT-001", "Updated description");

        service.handle(event);

        verify(activityRepository).applyUpdate(eq(event), any(Instant.class));
    }

    @Test
    void handle_ChangeActivityStatusRequested_RoutesToActivityRepository() {
        ChangeActivityStatusRequested event = new ChangeActivityStatusRequested("ACT-001", "Completed");

        service.handle(event);

        verify(activityRepository).changeStatus(eq(event), any(Instant.class));
    }

    @Test
    void handle_CompleteActivityRequested_RoutesToActivityRepository() {
        CompleteActivityRequested event = new CompleteActivityRequested("ACT-001", Instant.now());

        service.handle(event);

        verify(activityRepository).updateCompletedTime(eq("ACT-001"), any(Instant.class), any(Instant.class));
    }

    @Test
    void handle_LinkActivityToIncidentRequested_RoutesToActivityRepository() {
        LinkActivityToIncidentRequested event = new LinkActivityToIncidentRequested("ACT-001", "ACT-001", "INC-001");

        service.handle(event);

        verify(activityRepository).linkToIncident(eq(event), any(Instant.class));
    }

    // Assignment event tests
    @Test
    void handle_CreateAssignmentRequested_RoutesToAssignmentRepository() {
        CreateAssignmentRequested event = new CreateAssignmentRequested(
                "ASSIGN-001", "ASSIGN-001", Instant.now(), "Primary", "Assigned", "INC-001", null
        );

        service.handle(event);

        verify(assignmentRepository).upsert(eq(event), any(Instant.class));
    }

    @Test
    void handle_ChangeAssignmentStatusRequested_RoutesToAssignmentRepository() {
        ChangeAssignmentStatusRequested event = new ChangeAssignmentStatusRequested("ASSIGN-001", "Active");

        service.handle(event);

        verify(assignmentRepository).changeStatus(eq(event), any(Instant.class));
    }

    @Test
    void handle_CompleteAssignmentRequested_RoutesToAssignmentRepository() {
        CompleteAssignmentRequested event = new CompleteAssignmentRequested("ASSIGN-001", Instant.now());

        service.handle(event);

        verify(assignmentRepository).complete(eq(event), any(Instant.class));
    }

    @Test
    void handle_LinkAssignmentToDispatchRequested_RoutesToAssignmentRepository() {
        LinkAssignmentToDispatchRequested event = new LinkAssignmentToDispatchRequested(
                "ASSIGN-001", "ASSIGN-001", "DISP-001"
        );

        service.handle(event);

        verify(assignmentRepository).linkDispatch(eq(event), any(Instant.class));
    }

    // Involved party event tests
    @Test
    void handle_InvolvePartyRequested_RoutesToInvolvedPartyRepository() {
        InvolvePartyRequested event = new InvolvePartyRequested(
                "INV-001", "INV-001", "PERSON-001", "INC-001", null, null, "Victim", "Test", Instant.now()
        );

        service.handle(event);

        verify(involvedPartyRepository).upsert(eq(event), any(Instant.class));
    }

    @Test
    void handle_UpdatePartyInvolvementRequested_RoutesToInvolvedPartyRepository() {
        UpdatePartyInvolvementRequested event = new UpdatePartyInvolvementRequested(
                "INV-001", "INV-001", "Witness", "Updated description"
        );

        service.handle(event);

        verify(involvedPartyRepository).applyUpdate(eq(event), any(Instant.class));
    }

    @Test
    void handle_EndPartyInvolvementRequested_RoutesToInvolvedPartyRepository() {
        EndPartyInvolvementRequested event = new EndPartyInvolvementRequested("INV-001", "INV-001", Instant.now());

        service.handle(event);

        verify(involvedPartyRepository).endInvolvement(eq(event), any(Instant.class));
    }

    // Resource assignment event tests
    @Test
    void handle_AssignResourceRequested_RoutesToResourceAssignmentRepository() {
        AssignResourceRequested event = new AssignResourceRequested(
                "ASSIGN-001", "ASSIGN-001", "RES-001", "Officer", "Primary", "Active", Instant.now()
        );

        service.handle(event);

        verify(resourceAssignmentRepository).upsert(eq(event), any(Instant.class));
    }

    @Test
    void handle_ChangeResourceAssignmentStatusRequested_RoutesToResourceAssignmentRepository() {
        ChangeResourceAssignmentStatusRequested event = new ChangeResourceAssignmentStatusRequested(
                "ASSIGN-001", "ASSIGN-001", "RES-001", "Completed"
        );

        service.handle(event);

        verify(resourceAssignmentRepository).changeStatus(eq(event), any(Instant.class));
    }

    @Test
    void handle_UnassignResourceRequested_RoutesToResourceAssignmentRepository() {
        UnassignResourceRequested event = new UnassignResourceRequested(
                "ASSIGN-001", "ASSIGN-001", "RES-001", Instant.now()
        );

        service.handle(event);

        verify(resourceAssignmentRepository).unassign(eq(event), any(Instant.class));
    }

    @Test
    void handle_NullEvent_LogsWarning() {
        service.handle(null);

        // Should not throw exception, just log warning
        verify(incidentRepository, never()).upsert(any(), any());
    }

    @Test
    void handle_UnsupportedEvent_LogsWarning() {
        Object unsupportedEvent = new Object();

        service.handle(unsupportedEvent);

        // Should log warning but not throw exception
        verify(incidentRepository, never()).upsert(any(), any());
    }

    @Test
    void handle_UsesClockForTimestamp() {
        ReportIncidentRequested event = new ReportIncidentRequested(
                "INC-001", "2024-001", "High", "Reported", Instant.now(), "Test", "Traffic"
        );
        ArgumentCaptor<Instant> timestampCaptor = ArgumentCaptor.forClass(Instant.class);

        service.handle(event);

        verify(incidentRepository).upsert(eq(event), timestampCaptor.capture());
        assertThat(timestampCaptor.getValue()).isEqualTo(Instant.parse("2024-01-01T12:00:00Z"));
    }
}
