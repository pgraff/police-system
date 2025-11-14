package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.validation.ValidationResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Validation error response DTO.
 * Extends ErrorResponse with validation-specific formatting.
 */
public class ValidationErrorResponse extends ErrorResponse {
    /**
     * Creates a new validation error response.
     *
     * @param error the error type
     * @param message the error message
     * @param details the list of detailed error messages
     */
    public ValidationErrorResponse(String error, String message, List<String> details) {
        super(error, message, details);
    }

    /**
     * Creates a ValidationErrorResponse from a ValidationResult.
     *
     * @param validationResult the validation result
     * @return the validation error response, or null if validation passed
     */
    public static ValidationErrorResponse fromValidationResult(ValidationResult validationResult) {
        if (validationResult.isValid()) {
            return null;
        }

        List<String> details = validationResult.getErrors().stream()
                .map(error -> error.getField() + ": " + error.getMessage())
                .collect(Collectors.toList());

        return new ValidationErrorResponse("Bad Request", "Validation failed", details);
    }
}

