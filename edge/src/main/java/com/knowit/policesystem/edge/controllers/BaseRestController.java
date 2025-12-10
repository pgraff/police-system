package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.dto.SuccessResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Base REST controller for all API endpoints.
 * Provides common functionality and response helpers.
 * All REST controllers should extend this class.
 */
public abstract class BaseRestController {

    /**
     * Creates a success response with 200 OK status.
     *
     * @param <T> the type of the response data
     * @param data the response data
     * @param message the success message
     * @return ResponseEntity with SuccessResponse
     */
    protected <T> ResponseEntity<SuccessResponse<T>> success(T data, String message) {
        SuccessResponse<T> response = new SuccessResponse<>(message, data);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a success response with 201 Created status.
     *
     * @param <T> the type of the response data
     * @param data the response data
     * @param message the success message
     * @return ResponseEntity with SuccessResponse and 201 Created status
     */
    protected <T> ResponseEntity<SuccessResponse<T>> created(T data, String message) {
        SuccessResponse<T> response = new SuccessResponse<>(message, data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates a success response with 200 OK status and default message.
     *
     * @param <T> the type of the response data
     * @param data the response data
     * @return ResponseEntity with SuccessResponse
     */
    protected <T> ResponseEntity<SuccessResponse<T>> success(T data) {
        return success(data, "Request processed successfully");
    }
}

