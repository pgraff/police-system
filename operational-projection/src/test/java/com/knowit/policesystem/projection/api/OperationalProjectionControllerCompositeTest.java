package com.knowit.policesystem.projection.api;

import com.knowit.policesystem.projection.service.OperationalProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalProjectionControllerCompositeTest {

    @Mock
    private OperationalProjectionService projectionService;

    @InjectMocks
    private OperationalProjectionController controller;

    @Test
    void getIncidentFull_WithValidId_ReturnsOk() {
        String incidentId = "INC-001";
        IncidentProjectionResponse incident = new IncidentProjectionResponse(
                incidentId, "2024-001", "High", "Reported", Instant.now(), null, null, null, "Test", "Traffic", Instant.now()
        );
        CallProjectionResponse call = new CallProjectionResponse(
                "CALL-001", "CALL-001", "High", "Received", Instant.now(), null, null, null, "Test call", "Emergency", incidentId, Instant.now()
        );
        DispatchProjectionResponse dispatch = new DispatchProjectionResponse(
                "DISP-001", Instant.now(), "Initial", "Dispatched", "CALL-001", Instant.now()
        );
        ActivityProjectionResponse activity = new ActivityProjectionResponse(
                "ACT-001", Instant.now(), "Investigation", "Test activity", "InProgress", null, incidentId, Instant.now()
        );
        AssignmentProjectionResponse assignment = new AssignmentProjectionResponse(
                "ASSIGN-001", Instant.now(), "Primary", "Assigned", incidentId, null, null, null, Instant.now(), Instant.now()
        );
        ResourceAssignmentProjectionResponse resourceAssignment = new ResourceAssignmentProjectionResponse(
                1L, "ASSIGN-001", "RES-001", "Officer", "Primary", "Active", Instant.now(), null, Instant.now()
        );
        InvolvedPartyProjectionResponse involvedParty = new InvolvedPartyProjectionResponse(
                "INV-001", "PERSON-001", "Victim", "Test", Instant.now(), null, incidentId, null, null, Instant.now()
        );

        IncidentFullResponse fullResponse = new IncidentFullResponse(
                incident,
                List.of(new CallWithDispatchesResponse(call, List.of(dispatch))),
                List.of(activity),
                List.of(new AssignmentWithResourcesResponse(assignment, List.of(resourceAssignment))),
                List.of(involvedParty)
        );

        when(projectionService.getIncidentFull(incidentId)).thenReturn(Optional.of(fullResponse));

        ResponseEntity<IncidentFullResponse> result = controller.getIncidentFull(incidentId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().incident().incidentId()).isEqualTo(incidentId);
        assertThat(result.getBody().calls()).hasSize(1);
        assertThat(result.getBody().calls().get(0).call().callId()).isEqualTo("CALL-001");
        assertThat(result.getBody().calls().get(0).dispatches()).hasSize(1);
        assertThat(result.getBody().activities()).hasSize(1);
        assertThat(result.getBody().assignments()).hasSize(1);
        assertThat(result.getBody().assignments().get(0).assignment().assignmentId()).isEqualTo("ASSIGN-001");
        assertThat(result.getBody().assignments().get(0).resourceAssignments()).hasSize(1);
        assertThat(result.getBody().involvedParties()).hasSize(1);
    }

    @Test
    void getIncidentFull_WithNoRelatedEntities_ReturnsOkWithEmptyLists() {
        String incidentId = "INC-001";
        IncidentProjectionResponse incident = new IncidentProjectionResponse(
                incidentId, "2024-001", "High", "Reported", Instant.now(), null, null, null, "Test", "Traffic", Instant.now()
        );

        IncidentFullResponse fullResponse = new IncidentFullResponse(
                incident,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        when(projectionService.getIncidentFull(incidentId)).thenReturn(Optional.of(fullResponse));

        ResponseEntity<IncidentFullResponse> result = controller.getIncidentFull(incidentId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().incident().incidentId()).isEqualTo(incidentId);
        assertThat(result.getBody().calls()).isEmpty();
        assertThat(result.getBody().activities()).isEmpty();
        assertThat(result.getBody().assignments()).isEmpty();
        assertThat(result.getBody().involvedParties()).isEmpty();
    }

    @Test
    void getIncidentFull_WithInvalidId_ReturnsNotFound() {
        when(projectionService.getIncidentFull("INVALID")).thenReturn(Optional.empty());

        ResponseEntity<IncidentFullResponse> result = controller.getIncidentFull("INVALID");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getIncidentFull_WithMultipleCallsAndDispatches_ReturnsAll() {
        String incidentId = "INC-001";
        IncidentProjectionResponse incident = new IncidentProjectionResponse(
                incidentId, "2024-001", "High", "Reported", Instant.now(), null, null, null, "Test", "Traffic", Instant.now()
        );
        CallProjectionResponse call1 = new CallProjectionResponse(
                "CALL-001", "CALL-001", "High", "Received", Instant.now(), null, null, null, "Call 1", "Emergency", incidentId, Instant.now()
        );
        CallProjectionResponse call2 = new CallProjectionResponse(
                "CALL-002", "CALL-002", "Medium", "Received", Instant.now(), null, null, null, "Call 2", "NonEmergency", incidentId, Instant.now()
        );
        DispatchProjectionResponse dispatch1 = new DispatchProjectionResponse(
                "DISP-001", Instant.now(), "Initial", "Dispatched", "CALL-001", Instant.now()
        );
        DispatchProjectionResponse dispatch2 = new DispatchProjectionResponse(
                "DISP-002", Instant.now(), "FollowUp", "Dispatched", "CALL-001", Instant.now()
        );

        IncidentFullResponse fullResponse = new IncidentFullResponse(
                incident,
                List.of(
                        new CallWithDispatchesResponse(call1, List.of(dispatch1, dispatch2)),
                        new CallWithDispatchesResponse(call2, List.of())
                ),
                List.of(),
                List.of(),
                List.of()
        );

        when(projectionService.getIncidentFull(incidentId)).thenReturn(Optional.of(fullResponse));

        ResponseEntity<IncidentFullResponse> result = controller.getIncidentFull(incidentId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().calls()).hasSize(2);
        assertThat(result.getBody().calls().get(0).dispatches()).hasSize(2);
        assertThat(result.getBody().calls().get(1).dispatches()).isEmpty();
    }

    @Test
    void getIncidentFull_WithMultipleAssignmentsAndResources_ReturnsAll() {
        String incidentId = "INC-001";
        IncidentProjectionResponse incident = new IncidentProjectionResponse(
                incidentId, "2024-001", "High", "Reported", Instant.now(), null, null, null, "Test", "Traffic", Instant.now()
        );
        AssignmentProjectionResponse assignment1 = new AssignmentProjectionResponse(
                "ASSIGN-001", Instant.now(), "Primary", "Assigned", incidentId, null, null, null, Instant.now(), Instant.now()
        );
        AssignmentProjectionResponse assignment2 = new AssignmentProjectionResponse(
                "ASSIGN-002", Instant.now(), "Backup", "Assigned", incidentId, null, null, null, Instant.now(), Instant.now()
        );
        ResourceAssignmentProjectionResponse resource1 = new ResourceAssignmentProjectionResponse(
                1L, "ASSIGN-001", "RES-001", "Officer", "Primary", "Active", Instant.now(), null, Instant.now()
        );
        ResourceAssignmentProjectionResponse resource2 = new ResourceAssignmentProjectionResponse(
                2L, "ASSIGN-001", "RES-002", "Vehicle", "Primary", "Active", Instant.now(), null, Instant.now()
        );

        IncidentFullResponse fullResponse = new IncidentFullResponse(
                incident,
                List.of(),
                List.of(),
                List.of(
                        new AssignmentWithResourcesResponse(assignment1, List.of(resource1, resource2)),
                        new AssignmentWithResourcesResponse(assignment2, List.of())
                ),
                List.of()
        );

        when(projectionService.getIncidentFull(incidentId)).thenReturn(Optional.of(fullResponse));

        ResponseEntity<IncidentFullResponse> result = controller.getIncidentFull(incidentId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().assignments()).hasSize(2);
        assertThat(result.getBody().assignments().get(0).resourceAssignments()).hasSize(2);
        assertThat(result.getBody().assignments().get(1).resourceAssignments()).isEmpty();
    }
}
