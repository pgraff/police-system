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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OperationalProjectionControllerTest {

    @Mock
    private OperationalProjectionService projectionService;

    @InjectMocks
    private OperationalProjectionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // Incident endpoint tests
    @Test
    void getIncident_WithValidId_ReturnsOk() {
        IncidentProjectionResponse response = new IncidentProjectionResponse(
                "INC-001", "2024-001", "High", "Reported", Instant.now(), null, null, null, "Test", "Traffic", Instant.now()
        );
        when(projectionService.getIncident("INC-001")).thenReturn(Optional.of(response));

        ResponseEntity<IncidentProjectionResponse> result = controller.getIncident("INC-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().incidentId()).isEqualTo("INC-001");
    }

    @Test
    void getIncident_WithInvalidId_ReturnsNotFound() {
        when(projectionService.getIncident("INVALID")).thenReturn(Optional.empty());

        ResponseEntity<IncidentProjectionResponse> result = controller.getIncident("INVALID");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getIncidentHistory_WithValidId_ReturnsOk() throws Exception {
        IncidentProjectionResponse projection = new IncidentProjectionResponse(
                "INC-001", "2024-001", "High", "Reported", Instant.now(), null, null, null, "Test", "Traffic", Instant.now()
        );
        List<IncidentStatusHistoryResponse> history = List.of(
                new IncidentStatusHistoryResponse("Reported", Instant.now()),
                new IncidentStatusHistoryResponse("Active", Instant.now())
        );
        when(projectionService.getIncident("INC-001")).thenReturn(Optional.of(projection));
        when(projectionService.getIncidentHistory("INC-001")).thenReturn(history);

        mockMvc.perform(get("/api/projections/incidents/INC-001/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getIncidentHistory_WithInvalidId_ReturnsNotFound() {
        when(projectionService.getIncident("INVALID")).thenReturn(Optional.empty());

        ResponseEntity<List<IncidentStatusHistoryResponse>> result = controller.getIncidentHistory("INVALID");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listIncidents_WithFilters_ReturnsOk() {
        List<IncidentProjectionResponse> content = List.of(
                new IncidentProjectionResponse("INC-001", "2024-001", "High", "Reported", Instant.now(), null, null, null, "Test", "Traffic", Instant.now())
        );
        IncidentProjectionPageResponse pageResponse = new IncidentProjectionPageResponse(content, 1L, 0, 20);
        when(projectionService.listIncidents("Reported", "High", "Traffic", 0, 20)).thenReturn(pageResponse);

        ResponseEntity<IncidentProjectionPageResponse> result = controller.listIncidents("Reported", "High", "Traffic", 0, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().total()).isEqualTo(1L);
    }

    @Test
    void listIncidents_BoundsPagination() {
        List<IncidentProjectionResponse> content = List.of();
        IncidentProjectionPageResponse pageResponse = new IncidentProjectionPageResponse(content, 0L, 0, 1);
        when(projectionService.listIncidents(isNull(), isNull(), isNull(), eq(0), eq(1))).thenReturn(pageResponse);

        ResponseEntity<IncidentProjectionPageResponse> result = controller.listIncidents(null, null, null, -1, 0);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(projectionService).listIncidents(isNull(), isNull(), isNull(), eq(0), eq(1));
    }

    // Call endpoint tests
    @Test
    void getCall_WithValidId_ReturnsOk() {
        CallProjectionResponse response = new CallProjectionResponse(
                "CALL-001", "CALL-001", "High", "Received", Instant.now(), null, null, null, "Test", "Emergency", null, Instant.now()
        );
        when(projectionService.getCall("CALL-001")).thenReturn(Optional.of(response));

        ResponseEntity<CallProjectionResponse> result = controller.getCall("CALL-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().callId()).isEqualTo("CALL-001");
    }

    @Test
    void getCallHistory_WithValidId_ReturnsOk() {
        CallProjectionResponse projection = new CallProjectionResponse(
                "CALL-001", "CALL-001", "High", "Received", Instant.now(), null, null, null, "Test", "Emergency", null, Instant.now()
        );
        List<CallStatusHistoryResponse> history = List.of(
                new CallStatusHistoryResponse("Received", Instant.now())
        );
        when(projectionService.getCall("CALL-001")).thenReturn(Optional.of(projection));
        when(projectionService.getCallHistory("CALL-001")).thenReturn(history);

        ResponseEntity<List<CallStatusHistoryResponse>> result = controller.getCallHistory("CALL-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void listCalls_WithFilters_ReturnsOk() {
        List<CallProjectionResponse> content = List.of();
        CallProjectionPageResponse pageResponse = new CallProjectionPageResponse(content, 0L, 0, 20);
        when(projectionService.listCalls("Received", "High", "Emergency", 0, 20)).thenReturn(pageResponse);

        ResponseEntity<CallProjectionPageResponse> result = controller.listCalls("Received", "High", "Emergency", 0, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Dispatch endpoint tests
    @Test
    void getDispatch_WithValidId_ReturnsOk() {
        DispatchProjectionResponse response = new DispatchProjectionResponse(
                "DISP-001", Instant.now(), "Initial", "Dispatched", "CALL-001", Instant.now()
        );
        when(projectionService.getDispatch("DISP-001")).thenReturn(Optional.of(response));

        ResponseEntity<DispatchProjectionResponse> result = controller.getDispatch("DISP-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
    }

    @Test
    void getDispatchHistory_WithValidId_ReturnsOk() {
        DispatchProjectionResponse projection = new DispatchProjectionResponse(
                "DISP-001", Instant.now(), "Initial", "Dispatched", "CALL-001", Instant.now()
        );
        List<DispatchStatusHistoryResponse> history = List.of(
                new DispatchStatusHistoryResponse("Dispatched", Instant.now())
        );
        when(projectionService.getDispatch("DISP-001")).thenReturn(Optional.of(projection));
        when(projectionService.getDispatchHistory("DISP-001")).thenReturn(history);

        ResponseEntity<List<DispatchStatusHistoryResponse>> result = controller.getDispatchHistory("DISP-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listDispatches_WithFilters_ReturnsOk() {
        List<DispatchProjectionResponse> content = List.of();
        DispatchProjectionPageResponse pageResponse = new DispatchProjectionPageResponse(content, 0L, 0, 20);
        when(projectionService.listDispatches("Dispatched", "Initial", 0, 20)).thenReturn(pageResponse);

        ResponseEntity<DispatchProjectionPageResponse> result = controller.listDispatches("Dispatched", "Initial", 0, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Activity endpoint tests
    @Test
    void getActivity_WithValidId_ReturnsOk() {
        ActivityProjectionResponse response = new ActivityProjectionResponse(
                "ACT-001", Instant.now(), "Investigation", "Test", "InProgress", null, null, Instant.now()
        );
        when(projectionService.getActivity("ACT-001")).thenReturn(Optional.of(response));

        ResponseEntity<ActivityProjectionResponse> result = controller.getActivity("ACT-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getActivityHistory_WithValidId_ReturnsOk() {
        ActivityProjectionResponse projection = new ActivityProjectionResponse(
                "ACT-001", Instant.now(), "Investigation", "Test", "InProgress", null, null, Instant.now()
        );
        List<ActivityStatusHistoryResponse> history = List.of(
                new ActivityStatusHistoryResponse("InProgress", Instant.now())
        );
        when(projectionService.getActivity("ACT-001")).thenReturn(Optional.of(projection));
        when(projectionService.getActivityHistory("ACT-001")).thenReturn(history);

        ResponseEntity<List<ActivityStatusHistoryResponse>> result = controller.getActivityHistory("ACT-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listActivities_WithFilters_ReturnsOk() {
        List<ActivityProjectionResponse> content = List.of();
        ActivityProjectionPageResponse pageResponse = new ActivityProjectionPageResponse(content, 0L, 0, 20);
        when(projectionService.listActivities("InProgress", "Investigation", 0, 20)).thenReturn(pageResponse);

        ResponseEntity<ActivityProjectionPageResponse> result = controller.listActivities("InProgress", "Investigation", 0, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Assignment endpoint tests
    @Test
    void getAssignment_WithValidId_ReturnsOk() {
        AssignmentProjectionResponse response = new AssignmentProjectionResponse(
                "ASSIGN-001", Instant.now(), "Primary", "Assigned", "INC-001", null, null, null, Instant.now(), Instant.now()
        );
        when(projectionService.getAssignment("ASSIGN-001")).thenReturn(Optional.of(response));

        ResponseEntity<AssignmentProjectionResponse> result = controller.getAssignment("ASSIGN-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAssignmentHistory_WithValidId_ReturnsOk() {
        AssignmentProjectionResponse projection = new AssignmentProjectionResponse(
                "ASSIGN-001", Instant.now(), "Primary", "Assigned", "INC-001", null, null, null, Instant.now(), Instant.now()
        );
        List<AssignmentStatusHistoryResponse> history = List.of(
                new AssignmentStatusHistoryResponse("Assigned", Instant.now())
        );
        when(projectionService.getAssignment("ASSIGN-001")).thenReturn(Optional.of(projection));
        when(projectionService.getAssignmentHistory("ASSIGN-001")).thenReturn(history);

        ResponseEntity<List<AssignmentStatusHistoryResponse>> result = controller.getAssignmentHistory("ASSIGN-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listAssignments_WithFilters_ReturnsOk() {
        List<AssignmentProjectionResponse> content = List.of();
        AssignmentProjectionPageResponse pageResponse = new AssignmentProjectionPageResponse(content, 0L, 0, 20);
        when(projectionService.listAssignments("Assigned", "Primary", null, "INC-001", null, 0, 20)).thenReturn(pageResponse);

        ResponseEntity<AssignmentProjectionPageResponse> result = controller.listAssignments("Assigned", "Primary", null, "INC-001", null, 0, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Involved party endpoint tests
    @Test
    void getInvolvedParty_WithValidId_ReturnsOk() {
        InvolvedPartyProjectionResponse response = new InvolvedPartyProjectionResponse(
                "INV-001", "PERSON-001", "Victim", "Test", Instant.now(), null, "INC-001", null, null, Instant.now()
        );
        when(projectionService.getInvolvedParty("INV-001")).thenReturn(Optional.of(response));

        ResponseEntity<InvolvedPartyProjectionResponse> result = controller.getInvolvedParty("INV-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listInvolvedParties_WithFilters_ReturnsOk() {
        List<InvolvedPartyProjectionResponse> content = List.of();
        InvolvedPartyProjectionPageResponse pageResponse = new InvolvedPartyProjectionPageResponse(content, 0L, 0, 20);
        when(projectionService.listInvolvedParties("PERSON-001", "Victim", "INC-001", null, null, 0, 20)).thenReturn(pageResponse);

        ResponseEntity<InvolvedPartyProjectionPageResponse> result = controller.listInvolvedParties("PERSON-001", "Victim", "INC-001", null, null, 0, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Resource assignment endpoint tests
    @Test
    void getResourceAssignment_WithValidId_ReturnsOk() {
        ResourceAssignmentProjectionResponse response = new ResourceAssignmentProjectionResponse(
                1L, "ASSIGN-001", "RES-001", "Officer", "Primary", "Active", Instant.now(), null, Instant.now()
        );
        when(projectionService.getResourceAssignment(1L)).thenReturn(Optional.of(response));

        ResponseEntity<ResourceAssignmentProjectionResponse> result = controller.getResourceAssignment(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getResourceAssignment_WithInvalidId_ReturnsNotFound() {
        when(projectionService.getResourceAssignment(999L)).thenReturn(Optional.empty());

        ResponseEntity<ResourceAssignmentProjectionResponse> result = controller.getResourceAssignment(999L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listResourceAssignments_WithFilters_ReturnsOk() {
        List<ResourceAssignmentProjectionResponse> content = List.of();
        ResourceAssignmentProjectionPageResponse pageResponse = new ResourceAssignmentProjectionPageResponse(content, 0L, 0, 20);
        when(projectionService.listResourceAssignments("ASSIGN-001", "RES-001", "Officer", "Primary", "Active", 0, 20)).thenReturn(pageResponse);

        ResponseEntity<ResourceAssignmentProjectionPageResponse> result = controller.listResourceAssignments("ASSIGN-001", "RES-001", "Officer", "Primary", "Active", 0, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listIncidents_BoundsSizeToMax200() {
        List<IncidentProjectionResponse> content = List.of();
        IncidentProjectionPageResponse pageResponse = new IncidentProjectionPageResponse(content, 0L, 0, 200);
        when(projectionService.listIncidents(isNull(), isNull(), isNull(), eq(0), eq(200))).thenReturn(pageResponse);

        ResponseEntity<IncidentProjectionPageResponse> result = controller.listIncidents(null, null, null, 0, 300);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(projectionService).listIncidents(isNull(), isNull(), isNull(), eq(0), eq(200));
    }
}
