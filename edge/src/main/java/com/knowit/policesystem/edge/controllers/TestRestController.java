package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.dto.SuccessResponse;
import com.knowit.policesystem.edge.dto.TestRequestDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Test REST controller for infrastructure testing.
 * Provides endpoints to test validation, error handling, and response structure.
 * This controller is only active when the "test" profile is enabled.
 */
@RestController
@Profile("test")
public class TestRestController extends BaseRestController {

    /**
     * Test endpoint that accepts a validated request body.
     * Used to test request validation infrastructure.
     *
     * @param request the test request DTO
     * @return success response
     */
    @PostMapping("/test/validate")
    public ResponseEntity<SuccessResponse<TestRequestDto>> validateRequest(
            @Valid @RequestBody TestRequestDto request) {
        return success(request, "Validation passed");
    }

    /**
     * Test endpoint that returns a success response.
     * Used to test response structure and BaseRestController helpers.
     *
     * @return success response
     */
    @GetMapping("/test/success")
    public ResponseEntity<SuccessResponse<Map<String, String>>> success() {
        Map<String, String> data = Map.of("status", "ok", "message", "Test successful");
        return success(data, "Test endpoint working");
    }

    /**
     * Test endpoint that throws an exception.
     * Used to test error handling infrastructure.
     *
     * @return never returns (throws exception)
     */
    @GetMapping("/test/error")
    public ResponseEntity<SuccessResponse<String>> error() {
        throw new RuntimeException("Test error for error handling");
    }

    /**
     * Test endpoint that throws a validation exception.
     * Used to test validation exception handling.
     *
     * @return never returns (throws exception)
     */
    @GetMapping("/test/validation-error")
    public ResponseEntity<SuccessResponse<String>> validationError() {
        ValidationError error = new ValidationError("testField", "Test validation error", null);
        ValidationResult result = ValidationResult.builder()
                .addError(error)
                .build();
        throw new ValidationException(result);
    }

    /**
     * Test endpoint that throws an IllegalArgumentException.
     * Used to test IllegalArgumentException handling.
     *
     * @return never returns (throws exception)
     */
    @GetMapping("/test/illegal-argument")
    public ResponseEntity<SuccessResponse<String>> illegalArgument() {
        throw new IllegalArgumentException("Test illegal argument error");
    }
}
