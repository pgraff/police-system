package com.knowit.policesystem.edge.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check controller.
 * Provides a simple health endpoint to verify the API infrastructure is working.
 */
@RestController
public class HealthController extends BaseRestController {

    /**
     * Health check endpoint.
     * Returns a simple success response indicating the API is operational.
     *
     * @return success response with health status
     */
    @GetMapping("/health")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<Map<String, String>>> health() {
        Map<String, String> healthData = Map.of(
                "status", "UP",
                "service", "police-system-edge"
        );
        return success(healthData, "Service is healthy");
    }
}

