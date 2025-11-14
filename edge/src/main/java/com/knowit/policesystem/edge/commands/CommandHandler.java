package com.knowit.policesystem.edge.commands;

/**
 * Interface for handling commands.
 * Implementations should process commands and return results.
 *
 * @param <C> the command type
 * @param <R> the result type
 */
public interface CommandHandler<C extends Command, R> {
    /**
     * Handles the given command and returns a result.
     *
     * @param command the command to handle
     * @return the result of command execution
     */
    R handle(C command);

    /**
     * Returns the command type this handler can process.
     *
     * @return the command class
     */
    Class<C> getCommandType();
}

