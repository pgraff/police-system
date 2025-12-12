package com.knowit.policesystem.edge.validation.officers;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.officers.RegisterOfficerCommand;
import com.knowit.policesystem.edge.exceptions.ConflictException;
import com.knowit.policesystem.edge.services.officers.OfficerConflictService;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for RegisterOfficerCommand.
 * Validates that all required fields are present, enum values are valid, and checks for conflicts.
 */
@Component
public class RegisterOfficerCommandValidator extends CommandValidator {

    private final OfficerConflictService officerConflictService;

    public RegisterOfficerCommandValidator(OfficerConflictService officerConflictService) {
        this.officerConflictService = officerConflictService;
    }

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

        ValidationResult result = builder.build();
        if (!result.isValid()) {
            return result;
        }

        // Check for conflicts (duplicate badge number)
        if (officerConflictService.badgeNumberExists(registerCommand.getBadgeNumber())) {
            throw new ConflictException("Officer with badge number already exists: " + registerCommand.getBadgeNumber());
        }

        return result;
    }
}
