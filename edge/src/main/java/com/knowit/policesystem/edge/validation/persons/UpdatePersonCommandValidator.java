package com.knowit.policesystem.edge.validation.persons;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.persons.UpdatePersonCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for UpdatePersonCommand.
 * Validates that personId is present.
 * Enum validation is handled by Jackson deserialization at the DTO level.
 */
@Component
public class UpdatePersonCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof UpdatePersonCommand)) {
            return ValidationResult.valid();
        }

        UpdatePersonCommand updateCommand = (UpdatePersonCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate personId
        if (updateCommand.getPersonId() == null || updateCommand.getPersonId().trim().isEmpty()) {
            builder.addError(new ValidationError("personId", "personId is required", updateCommand.getPersonId()));
        }

        return builder.build();
    }
}
