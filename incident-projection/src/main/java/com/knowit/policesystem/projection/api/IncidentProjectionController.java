package com.knowit.policesystem.projection.api;

import com.knowit.policesystem.projection.service.IncidentProjectionService;
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
@RequestMapping("/api/projections/incidents")
public class IncidentProjectionController {

    private final IncidentProjectionService projectionService;

    public IncidentProjectionController(IncidentProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentProjectionResponse> getIncident(@PathVariable String incidentId) {
        return projectionService.getProjection(incidentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{incidentId}/history")
    public ResponseEntity<List<IncidentStatusHistoryResponse>> getHistory(@PathVariable String incidentId) {
        Optional<IncidentProjectionResponse> projection = projectionService.getProjection(incidentId);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<IncidentStatusHistoryResponse> history = projectionService.getHistory(incidentId);
        return ResponseEntity.ok(history);
    }

    @GetMapping
    public ResponseEntity<IncidentProjectionPageResponse> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "priority", required = false) String priority,
            @RequestParam(name = "incidentType", required = false) String incidentType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        IncidentProjectionPageResponse response = projectionService.list(status, priority, incidentType, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }
}
