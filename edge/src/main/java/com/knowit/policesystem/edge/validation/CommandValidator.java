package com.knowit.policesystem.edge.validation;

import com.knowit.policesystem.edge.commands.Command;

/**
 * Base abstract class for command validators.
 * Provides common validation utilities for commands.
 */
public abstract class CommandValidator implements Validator<Command> {
    /**
     * Validates the given command.
     *
     * @param command the command to validate
     * @return the validation result
     */
    @Override
    public abstract ValidationResult validate(Command command);
}

