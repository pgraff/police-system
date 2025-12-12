package com.knowit.policesystem.projection.api;

import com.knowit.policesystem.projection.service.CallProjectionService;
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
@RequestMapping("/api/projections/calls")
public class CallProjectionController {

    private final CallProjectionService projectionService;

    public CallProjectionController(CallProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @GetMapping("/{callId}")
    public ResponseEntity<CallProjectionResponse> getCall(@PathVariable String callId) {
        return projectionService.getProjection(callId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{callId}/history")
    public ResponseEntity<List<CallStatusHistoryResponse>> getHistory(@PathVariable String callId) {
        Optional<CallProjectionResponse> projection = projectionService.getProjection(callId);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<CallStatusHistoryResponse> history = projectionService.getHistory(callId);
        return ResponseEntity.ok(history);
    }

    @GetMapping
    public ResponseEntity<CallProjectionPageResponse> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "priority", required = false) String priority,
            @RequestParam(name = "callType", required = false) String callType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        CallProjectionPageResponse response = projectionService.list(status, priority, callType, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }
}

