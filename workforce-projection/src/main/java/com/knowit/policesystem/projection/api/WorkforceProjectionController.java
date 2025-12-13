package com.knowit.policesystem.projection.api;

import com.knowit.policesystem.projection.service.WorkforceProjectionService;
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
public class WorkforceProjectionController {

    private final WorkforceProjectionService projectionService;

    public WorkforceProjectionController(WorkforceProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    // Shift endpoints
    @GetMapping("/shifts/{shiftId}")
    public ResponseEntity<ShiftProjectionResponse> getShift(@PathVariable String shiftId) {
        return projectionService.getShift(shiftId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/shifts/{shiftId}/history")
    public ResponseEntity<List<ShiftStatusHistoryResponse>> getShiftHistory(@PathVariable String shiftId) {
        Optional<ShiftProjectionResponse> projection = projectionService.getShift(shiftId);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<ShiftStatusHistoryResponse> history = projectionService.getShiftHistory(shiftId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/shifts")
    public ResponseEntity<ShiftProjectionPageResponse> listShifts(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "shiftType", required = false) String shiftType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        ShiftProjectionPageResponse response = projectionService.listShifts(status, shiftType, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }

    // Officer shift endpoints
    @GetMapping("/officer-shifts/{id}")
    public ResponseEntity<OfficerShiftProjectionResponse> getOfficerShift(@PathVariable Long id) {
        return projectionService.getOfficerShift(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/officer-shifts")
    public ResponseEntity<OfficerShiftProjectionPageResponse> listOfficerShifts(
            @RequestParam(name = "shiftId", required = false) String shiftId,
            @RequestParam(name = "badgeNumber", required = false) String badgeNumber,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        OfficerShiftProjectionPageResponse response = projectionService.listOfficerShifts(shiftId, badgeNumber, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }

    // Shift change endpoints
    @GetMapping("/shift-changes/{shiftChangeId}")
    public ResponseEntity<ShiftChangeProjectionResponse> getShiftChange(@PathVariable String shiftChangeId) {
        return projectionService.getShiftChange(shiftChangeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/shift-changes")
    public ResponseEntity<ShiftChangeProjectionPageResponse> listShiftChanges(
            @RequestParam(name = "shiftId", required = false) String shiftId,
            @RequestParam(name = "changeType", required = false) String changeType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        ShiftChangeProjectionPageResponse response = projectionService.listShiftChanges(shiftId, changeType, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }
}
