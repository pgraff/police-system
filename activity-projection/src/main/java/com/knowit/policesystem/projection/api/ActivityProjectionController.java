package com.knowit.policesystem.projection.api;

import com.knowit.policesystem.projection.service.ActivityProjectionService;
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
@RequestMapping("/api/projections/activities")
public class ActivityProjectionController {

    private final ActivityProjectionService projectionService;

    public ActivityProjectionController(ActivityProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @GetMapping("/{activityId}")
    public ResponseEntity<ActivityProjectionResponse> getActivity(@PathVariable String activityId) {
        return projectionService.getProjection(activityId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{activityId}/history")
    public ResponseEntity<List<ActivityStatusHistoryResponse>> getHistory(@PathVariable String activityId) {
        Optional<ActivityProjectionResponse> projection = projectionService.getProjection(activityId);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<ActivityStatusHistoryResponse> history = projectionService.getHistory(activityId);
        return ResponseEntity.ok(history);
    }

    @GetMapping
    public ResponseEntity<ActivityProjectionPageResponse> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "activityType", required = false) String activityType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        ActivityProjectionPageResponse response = projectionService.list(status, activityType, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }
}

