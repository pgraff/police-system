package com.knowit.policesystem.edge.exceptions;

/**
 * Exception representing a missing resource.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
