package com.knowit.policesystem.edge.exceptions;

import com.knowit.policesystem.edge.validation.ValidationResult;

/**
 * Exception thrown when command validation fails.
 * Contains the validation result with error details.
 */
public class ValidationException extends RuntimeException {
    private final ValidationResult validationResult;

    /**
     * Creates a new validation exception.
     *
     * @param validationResult the validation result containing errors
     */
    public ValidationException(ValidationResult validationResult) {
        super("Validation failed: " + validationResult.getErrors().size() + " error(s)");
        this.validationResult = validationResult;
    }

    /**
     * Returns the validation result.
     *
     * @return the validation result
     */
    public ValidationResult getValidationResult() {
        return validationResult;
    }
}

