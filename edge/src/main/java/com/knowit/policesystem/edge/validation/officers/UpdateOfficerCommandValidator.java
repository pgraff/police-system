package com.knowit.policesystem.edge.validation.officers;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.officers.UpdateOfficerCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for UpdateOfficerCommand.
 * Validates that badgeNumber is present and email format is valid if provided.
 */
@Component
public class UpdateOfficerCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof UpdateOfficerCommand)) {
            return ValidationResult.valid();
        }

        UpdateOfficerCommand updateCommand = (UpdateOfficerCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate badgeNumber
        if (updateCommand.getBadgeNumber() == null || updateCommand.getBadgeNumber().trim().isEmpty()) {
            builder.addError(new ValidationError("badgeNumber", "badgeNumber is required", updateCommand.getBadgeNumber()));
        }

        // Validate email format if provided (basic check - @Email annotation on DTO handles detailed validation)
        if (updateCommand.getEmail() != null && !updateCommand.getEmail().trim().isEmpty()) {
            if (!updateCommand.getEmail().contains("@")) {
                builder.addError(new ValidationError("email", "email must be a valid email address", updateCommand.getEmail()));
            }
        }

        return builder.build();
    }
}
