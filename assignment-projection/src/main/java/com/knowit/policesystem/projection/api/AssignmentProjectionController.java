package com.knowit.policesystem.projection.api;

import com.knowit.policesystem.projection.service.AssignmentProjectionService;
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
@RequestMapping("/api/projections/assignments")
public class AssignmentProjectionController {

    private final AssignmentProjectionService projectionService;

    public AssignmentProjectionController(AssignmentProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @GetMapping("/{assignmentId}")
    public ResponseEntity<AssignmentProjectionResponse> getAssignment(@PathVariable String assignmentId) {
        return projectionService.getProjection(assignmentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{assignmentId}/history")
    public ResponseEntity<List<AssignmentStatusHistoryResponse>> getHistory(@PathVariable String assignmentId) {
        Optional<AssignmentProjectionResponse> projection = projectionService.getProjection(assignmentId);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<AssignmentStatusHistoryResponse> history = projectionService.getHistory(assignmentId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{assignmentId}/resources")
    public ResponseEntity<List<AssignmentResourceResponse>> getResources(@PathVariable String assignmentId) {
        Optional<AssignmentProjectionResponse> projection = projectionService.getProjection(assignmentId);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<AssignmentResourceResponse> resources = projectionService.getResources(assignmentId);
        return ResponseEntity.ok(resources);
    }

    @GetMapping
    public ResponseEntity<AssignmentProjectionPageResponse> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "assignmentType", required = false) String assignmentType,
            @RequestParam(name = "dispatchId", required = false) String dispatchId,
            @RequestParam(name = "incidentId", required = false) String incidentId,
            @RequestParam(name = "callId", required = false) String callId,
            @RequestParam(name = "resourceId", required = false) String resourceId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        AssignmentProjectionPageResponse response = projectionService.list(
                status, assignmentType, dispatchId, incidentId, callId, resourceId, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }
}

