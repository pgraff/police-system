package com.knowit.policesystem.edge.exceptions;

/**
 * Exception thrown when no command handler is found for a command type.
 */
public class CommandHandlerNotFoundException extends RuntimeException {
    /**
     * Creates a new exception.
     *
     * @param commandType the command type that has no handler
     */
    public CommandHandlerNotFoundException(String commandType) {
        super("No command handler found for command type: " + commandType);
    }
}

