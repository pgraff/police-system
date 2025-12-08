package com.knowit.policesystem.edge.validation.persons;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.persons.RegisterPersonCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for RegisterPersonCommand.
 * Validates that all required fields are present.
 */
@Component
public class RegisterPersonCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof RegisterPersonCommand)) {
            return ValidationResult.valid();
        }

        RegisterPersonCommand registerCommand = (RegisterPersonCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate personId
        if (registerCommand.getPersonId() == null || registerCommand.getPersonId().trim().isEmpty()) {
            builder.addError(new ValidationError("personId", "personId is required", registerCommand.getPersonId()));
        }

        // Validate firstName
        if (registerCommand.getFirstName() == null || registerCommand.getFirstName().trim().isEmpty()) {
            builder.addError(new ValidationError("firstName", "firstName is required", registerCommand.getFirstName()));
        }

        // Validate lastName
        if (registerCommand.getLastName() == null || registerCommand.getLastName().trim().isEmpty()) {
            builder.addError(new ValidationError("lastName", "lastName is required", registerCommand.getLastName()));
        }

        // Validate dateOfBirth
        if (registerCommand.getDateOfBirth() == null) {
            builder.addError(new ValidationError("dateOfBirth", "dateOfBirth is required", null));
        }

        return builder.build();
    }
}
