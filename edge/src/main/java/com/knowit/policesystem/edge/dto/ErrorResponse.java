package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Standard error response DTO.
 * Used for returning error information to clients.
 * Matches the ErrorResponse schema in the OpenAPI specification.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String error;
    private String message;
    private List<String> details;

    /**
     * Default constructor for Jackson deserialization.
     */
    public ErrorResponse() {
    }

    /**
     * Creates a new error response.
     *
     * @param error the error type/category
     * @param message the human-readable error message
     * @param details the list of detailed error messages
     */
    public ErrorResponse(String error, String message, List<String> details) {
        this.error = error;
        this.message = message;
        this.details = details != null ? List.copyOf(details) : List.of();
    }

    /**
     * Returns the error type/category.
     *
     * @return the error type
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error type.
     *
     * @param error the error type
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Returns the human-readable error message.
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
     * Returns the list of detailed error messages.
     *
     * @return the error details
     */
    public List<String> getDetails() {
        return details;
    }

    /**
     * Sets the error details.
     *
     * @param details the error details
     */
    public void setDetails(List<String> details) {
        this.details = details != null ? List.copyOf(details) : List.of();
    }
}

