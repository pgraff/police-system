package com.knowit.policesystem.edge.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container for validation results.
 * Contains a list of validation errors if validation failed.
 */
public class ValidationResult {
    private final boolean valid;
    private final List<ValidationError> errors;

    private ValidationResult(boolean valid, List<ValidationError> errors) {
        this.valid = valid;
        this.errors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : Collections.emptyList();
    }

    /**
     * Creates a valid validation result.
     *
     * @return a valid result
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, Collections.emptyList());
    }

    /**
     * Creates an invalid validation result with errors.
     *
     * @param errors the validation errors
     * @return an invalid result
     */
    public static ValidationResult withErrors(List<ValidationError> errors) {
        return new ValidationResult(false, errors);
    }

    /**
     * Returns a builder for creating validation results.
     *
     * @return a builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if validation passed.
     *
     * @return true if valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns true if validation failed (has errors).
     *
     * @return true if has errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Returns the list of validation errors.
     *
     * @return the list of errors
     */
    public List<ValidationError> getErrors() {
        return errors;
    }

    /**
     * Builder for creating ValidationResult instances.
     */
    public static class Builder {
        private final List<ValidationError> errors = new ArrayList<>();

        /**
         * Adds a validation error.
         *
         * @param error the validation error
         * @return this builder
         */
        public Builder addError(ValidationError error) {
            errors.add(error);
            return this;
        }

        /**
         * Builds the validation result.
         *
         * @return the validation result
         */
        public ValidationResult build() {
            if (errors.isEmpty()) {
                return ValidationResult.valid();
            }
            return ValidationResult.withErrors(errors);
        }
    }
}

