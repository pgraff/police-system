package com.knowit.policesystem.projection.api;

import com.knowit.policesystem.projection.service.ResourceProjectionService;
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
public class ResourceProjectionController {

    private final ResourceProjectionService projectionService;

    public ResourceProjectionController(ResourceProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    // Officer endpoints
    @GetMapping("/officers/{badgeNumber}")
    public ResponseEntity<OfficerProjectionResponse> getOfficer(@PathVariable String badgeNumber) {
        return projectionService.getOfficer(badgeNumber)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/officers/{badgeNumber}/history")
    public ResponseEntity<List<OfficerStatusHistoryResponse>> getOfficerHistory(@PathVariable String badgeNumber) {
        Optional<OfficerProjectionResponse> projection = projectionService.getOfficer(badgeNumber);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<OfficerStatusHistoryResponse> history = projectionService.getOfficerHistory(badgeNumber);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/officers")
    public ResponseEntity<OfficerProjectionPageResponse> listOfficers(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "rank", required = false) String rank,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        OfficerProjectionPageResponse response = projectionService.listOfficers(status, rank, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }

    // Vehicle endpoints
    @GetMapping("/vehicles/{unitId}")
    public ResponseEntity<VehicleProjectionResponse> getVehicle(@PathVariable String unitId) {
        return projectionService.getVehicle(unitId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/vehicles/{unitId}/history")
    public ResponseEntity<List<VehicleStatusHistoryResponse>> getVehicleHistory(@PathVariable String unitId) {
        Optional<VehicleProjectionResponse> projection = projectionService.getVehicle(unitId);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<VehicleStatusHistoryResponse> history = projectionService.getVehicleHistory(unitId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/vehicles")
    public ResponseEntity<VehicleProjectionPageResponse> listVehicles(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "vehicleType", required = false) String vehicleType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        VehicleProjectionPageResponse response = projectionService.listVehicles(status, vehicleType, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }

    // Unit endpoints
    @GetMapping("/units/{unitId}")
    public ResponseEntity<UnitProjectionResponse> getUnit(@PathVariable String unitId) {
        return projectionService.getUnit(unitId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/units/{unitId}/history")
    public ResponseEntity<List<UnitStatusHistoryResponse>> getUnitHistory(@PathVariable String unitId) {
        Optional<UnitProjectionResponse> projection = projectionService.getUnit(unitId);
        if (projection.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<UnitStatusHistoryResponse> history = projectionService.getUnitHistory(unitId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/units")
    public ResponseEntity<UnitProjectionPageResponse> listUnits(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "unitType", required = false) String unitType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        UnitProjectionPageResponse response = projectionService.listUnits(status, unitType, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }

    // Person endpoints
    @GetMapping("/persons/{personId}")
    public ResponseEntity<PersonProjectionResponse> getPerson(@PathVariable String personId) {
        return projectionService.getPerson(personId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/persons")
    public ResponseEntity<PersonProjectionPageResponse> listPersons(
            @RequestParam(name = "lastName", required = false) String lastName,
            @RequestParam(name = "firstName", required = false) String firstName,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        PersonProjectionPageResponse response = projectionService.listPersons(lastName, firstName, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }

    // Location endpoints
    @GetMapping("/locations/{locationId}")
    public ResponseEntity<LocationProjectionResponse> getLocation(@PathVariable String locationId) {
        return projectionService.getLocation(locationId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/locations")
    public ResponseEntity<LocationProjectionPageResponse> listLocations(
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 200));
        int boundedPage = Math.max(page, 0);
        LocationProjectionPageResponse response = projectionService.listLocations(city, state, boundedPage, boundedSize);
        return ResponseEntity.ok(response);
    }
}
