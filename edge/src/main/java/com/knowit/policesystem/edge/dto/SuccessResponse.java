package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard success response DTO.
 * Used for returning successful operation results to clients.
 *
 * @param <T> the type of the response data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuccessResponse<T> {
    private boolean success;
    private String message;
    private T data;

    /**
     * Default constructor for Jackson deserialization.
     */
    public SuccessResponse() {
    }

    /**
     * Creates a new success response.
     *
     * @param message the success message
     * @param data the response data
     */
    public SuccessResponse(String message, T data) {
        this.success = true;
        this.message = message;
        this.data = data;
    }

    /**
     * Returns true indicating success.
     *
     * @return always true
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success flag.
     *
     * @param success the success flag
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Returns the success message.
     *
     * @return the success message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the success message.
     *
     * @param message the success message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the response data.
     *
     * @return the response data
     */
    public T getData() {
        return data;
    }

    /**
     * Sets the response data.
     *
     * @param data the response data
     */
    public void setData(T data) {
        this.data = data;
    }
}

