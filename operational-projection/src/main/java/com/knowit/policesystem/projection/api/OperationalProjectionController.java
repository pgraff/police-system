package com.knowit.policesystem.projection.api;

import com.knowit.policesystem.projection.api.AssignmentWithResourcesResponse;
import com.knowit.policesystem.projection.api.CallWithDispatchesResponse;
import com.knowit.policesystem.projection.api.IncidentFullResponse;
import com.knowit.policesystem.projection.service.OperationalProjectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@Validated
@RequestMapping("/api/projections")
public class OperationalProjectionController {

    private final OperationalProjectionService projectionService;

    public OperationalProjectionController(OperationalProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    // Incident endpoints
    @GetMapping("/incidents/{incidentId}")
    public ResponseEntity<IncidentProjectionResponse> getIncident(@PathVariable String incidentId) {
        return projectionService.getIncident(incidentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/incidents/{incidentId}/history")
    public ResponseEntity<List<IncidentStatusHistoryResponse>> getIncidentHistory(@PathVariable String incidentId) {
        Optional<IncidentProjectionResponse> projection = projectionService.getIncident(incidentId);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<IncidentStatusHistoryResponse> history = projectionService.getIncidentHistory(incidentId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/incidents")
    public ResponseEntity<IncidentProjectionPageResponse> listIncidents(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "priority", required = false) String priority,
            @RequestParam(name = "incidentType", required = false) String incidentType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        IncidentProjectionPageResponse response = projectionService.listIncidents(status, priority, incidentType, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/incidents/{incidentId}/full")
    public ResponseEntity<IncidentFullResponse> getIncidentFull(@PathVariable String incidentId) {
        return projectionService.getIncidentFull(incidentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Call endpoints
    @GetMapping("/calls/{callId}")
    public ResponseEntity<CallProjectionResponse> getCall(@PathVariable String callId) {
        return projectionService.getCall(callId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/calls/{callId}/history")
    public ResponseEntity<List<CallStatusHistoryResponse>> getCallHistory(@PathVariable String callId) {
        Optional<CallProjectionResponse> projection = projectionService.getCall(callId);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<CallStatusHistoryResponse> history = projectionService.getCallHistory(callId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/calls")
    public ResponseEntity<CallProjectionPageResponse> listCalls(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "priority", required = false) String priority,
            @RequestParam(name = "callType", required = false) String callType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        CallProjectionPageResponse response = projectionService.listCalls(status, priority, callType, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }

    // Dispatch endpoints
    @GetMapping("/dispatches/{dispatchId}")
    public ResponseEntity<DispatchProjectionResponse> getDispatch(@PathVariable String dispatchId) {
        return projectionService.getDispatch(dispatchId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/dispatches/{dispatchId}/history")
    public ResponseEntity<List<DispatchStatusHistoryResponse>> getDispatchHistory(@PathVariable String dispatchId) {
        Optional<DispatchProjectionResponse> projection = projectionService.getDispatch(dispatchId);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<DispatchStatusHistoryResponse> history = projectionService.getDispatchHistory(dispatchId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/dispatches")
    public ResponseEntity<DispatchProjectionPageResponse> listDispatches(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "dispatchType", required = false) String dispatchType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        DispatchProjectionPageResponse response = projectionService.listDispatches(status, dispatchType, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }

    // Activity endpoints
    @GetMapping("/activities/{activityId}")
    public ResponseEntity<ActivityProjectionResponse> getActivity(@PathVariable String activityId) {
        return projectionService.getActivity(activityId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/activities/{activityId}/history")
    public ResponseEntity<List<ActivityStatusHistoryResponse>> getActivityHistory(@PathVariable String activityId) {
        Optional<ActivityProjectionResponse> projection = projectionService.getActivity(activityId);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<ActivityStatusHistoryResponse> history = projectionService.getActivityHistory(activityId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/activities")
    public ResponseEntity<ActivityProjectionPageResponse> listActivities(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "activityType", required = false) String activityType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        ActivityProjectionPageResponse response = projectionService.listActivities(status, activityType, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }

    // Assignment endpoints
    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<AssignmentProjectionResponse> getAssignment(@PathVariable String assignmentId) {
        return projectionService.getAssignment(assignmentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/assignments/{assignmentId}/history")
    public ResponseEntity<List<AssignmentStatusHistoryResponse>> getAssignmentHistory(@PathVariable String assignmentId) {
        Optional<AssignmentProjectionResponse> projection = projectionService.getAssignment(assignmentId);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<AssignmentStatusHistoryResponse> history = projectionService.getAssignmentHistory(assignmentId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/assignments")
    public ResponseEntity<AssignmentProjectionPageResponse> listAssignments(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "assignmentType", required = false) String assignmentType,
            @RequestParam(name = "dispatchId", required = false) String dispatchId,
            @RequestParam(name = "incidentId", required = false) String incidentId,
            @RequestParam(name = "callId", required = false) String callId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        AssignmentProjectionPageResponse response = projectionService.listAssignments(status, assignmentType, dispatchId, incidentId, callId, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }

    // Involved party endpoints
    @GetMapping("/involved-parties/{involvementId}")
    public ResponseEntity<InvolvedPartyProjectionResponse> getInvolvedParty(@PathVariable String involvementId) {
        return projectionService.getInvolvedParty(involvementId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/involved-parties")
    public ResponseEntity<InvolvedPartyProjectionPageResponse> listInvolvedParties(
            @RequestParam(name = "personId", required = false) String personId,
            @RequestParam(name = "partyRoleType", required = false) String partyRoleType,
            @RequestParam(name = "incidentId", required = false) String incidentId,
            @RequestParam(name = "callId", required = false) String callId,
            @RequestParam(name = "activityId", required = false) String activityId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        InvolvedPartyProjectionPageResponse response = projectionService.listInvolvedParties(personId, partyRoleType, incidentId, callId, activityId, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }

    // Resource assignment endpoints
    @GetMapping("/resource-assignments/{id}")
    public ResponseEntity<ResourceAssignmentProjectionResponse> getResourceAssignment(@PathVariable Long id) {
        return projectionService.getResourceAssignment(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/resource-assignments")
    public ResponseEntity<ResourceAssignmentProjectionPageResponse> listResourceAssignments(
            @RequestParam(name = "assignmentId", required = false) String assignmentId,
            @RequestParam(name = "resourceId", required = false) String resourceId,
            @RequestParam(name = "resourceType", required = false) String resourceType,
            @RequestParam(name = "roleType", required = false) String roleType,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        ResourceAssignmentProjectionPageResponse response = projectionService.listResourceAssignments(assignmentId, resourceId, resourceType, roleType, status, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }
}
