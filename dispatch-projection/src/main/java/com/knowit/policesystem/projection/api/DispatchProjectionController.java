package com.knowit.policesystem.projection.api;

import com.knowit.policesystem.projection.service.DispatchProjectionService;
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
@RequestMapping("/api/projections/dispatches")
public class DispatchProjectionController {

    private final DispatchProjectionService projectionService;

    public DispatchProjectionController(DispatchProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @GetMapping("/{dispatchId}")
    public ResponseEntity<DispatchProjectionResponse> getDispatch(@PathVariable String dispatchId) {
        return projectionService.getProjection(dispatchId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{dispatchId}/history")
    public ResponseEntity<List<DispatchStatusHistoryResponse>> getHistory(@PathVariable String dispatchId) {
        Optional<DispatchProjectionResponse> projection = projectionService.getProjection(dispatchId);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<DispatchStatusHistoryResponse> history = projectionService.getHistory(dispatchId);
        return ResponseEntity.ok(history);
    }

    @GetMapping
    public ResponseEntity<DispatchProjectionPageResponse> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "dispatchType", required = false) String dispatchType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        DispatchProjectionPageResponse response = projectionService.list(status, dispatchType, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }
}

