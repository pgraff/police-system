package com.knowit.policesystem.edge.validation;

/**
 * Interface for validating objects.
 *
 * @param <T> the type to validate
 */
public interface Validator<T> {
    /**
     * Validates the given object.
     *
     * @param object the object to validate
     * @return the validation result
     */
    ValidationResult validate(T object);
}

