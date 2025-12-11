package com.knowit.policesystem.projection.api;

import com.knowit.policesystem.projection.service.OfficerProjectionService;
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
@RequestMapping("/api/projections/officers")
public class OfficerProjectionController {

    private final OfficerProjectionService projectionService;

    public OfficerProjectionController(OfficerProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @GetMapping("/{badgeNumber}")
    public ResponseEntity<OfficerProjectionResponse> getOfficer(@PathVariable String badgeNumber) {
        return projectionService.getProjection(badgeNumber)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{badgeNumber}/history")
    public ResponseEntity<List<OfficerStatusHistoryResponse>> getHistory(@PathVariable String badgeNumber) {
        Optional<OfficerProjectionResponse> projection = projectionService.getProjection(badgeNumber);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<OfficerStatusHistoryResponse> history = projectionService.getHistory(badgeNumber);
        return ResponseEntity.ok(history);
    }

    @GetMapping
    public ResponseEntity<OfficerProjectionPageResponse> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "rank", required = false) String rank,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        OfficerProjectionPageResponse response = projectionService.list(status, rank, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }
}
