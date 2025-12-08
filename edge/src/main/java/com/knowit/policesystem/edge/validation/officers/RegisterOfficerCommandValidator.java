package com.knowit.policesystem.edge.validation.officers;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.officers.RegisterOfficerCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for RegisterOfficerCommand.
 * Validates that all required fields are present and enum values are valid.
 */
@Component
public class RegisterOfficerCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof RegisterOfficerCommand)) {
            return ValidationResult.valid();
        }

        RegisterOfficerCommand registerCommand = (RegisterOfficerCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate badgeNumber
        if (registerCommand.getBadgeNumber() == null || registerCommand.getBadgeNumber().trim().isEmpty()) {
            builder.addError(new ValidationError("badgeNumber", "badgeNumber is required", registerCommand.getBadgeNumber()));
        }

        // Validate email format (basic check - @Email annotation on DTO handles detailed validation)
        if (registerCommand.getEmail() != null && !registerCommand.getEmail().trim().isEmpty()) {
            if (!registerCommand.getEmail().contains("@")) {
                builder.addError(new ValidationError("email", "email must be a valid email address", registerCommand.getEmail()));
            }
        }

        // Validate status enum
        if (registerCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        return builder.build();
    }
}
