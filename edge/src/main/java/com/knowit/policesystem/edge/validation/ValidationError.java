package com.knowit.policesystem.edge.validation;

/**
 * Represents a single validation error for a field.
 */
public class ValidationError {
    private String field;
    private String message;
    private Object rejectedValue;

    /**
     * Creates a new validation error.
     *
     * @param field the field name that failed validation
     * @param message the error message
     * @param rejectedValue the value that was rejected
     */
    public ValidationError(String field, String message, Object rejectedValue) {
        this.field = field;
        this.message = message;
        this.rejectedValue = rejectedValue;
    }

    /**
     * Returns the field name that failed validation.
     *
     * @return the field name
     */
    public String getField() {
        return field;
    }

    /**
     * Sets the field name.
     *
     * @param field the field name
     */
    public void setField(String field) {
        this.field = field;
    }

    /**
     * Returns the error message.
     *
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the error message.
     *
     * @param message the error message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the value that was rejected.
     *
     * @return the rejected value
     */
    public Object getRejectedValue() {
        return rejectedValue;
    }

    /**
     * Sets the rejected value.
     *
     * @param rejectedValue the rejected value
     */
    public void setRejectedValue(Object rejectedValue) {
        this.rejectedValue = rejectedValue;
    }
}

