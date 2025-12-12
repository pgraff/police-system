package com.knowit.policesystem.edge.exceptions;

/**
 * Exception thrown when a resource conflict is detected (e.g., duplicate badge number).
 * Results in a 409 Conflict HTTP response.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

