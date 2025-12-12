package com.knowit.policesystem.edge.validation.officers;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.officers.UpdateOfficerCommand;
import com.knowit.policesystem.edge.exceptions.NotFoundException;
import com.knowit.policesystem.edge.services.officers.OfficerExistenceService;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for UpdateOfficerCommand.
 * Validates that badgeNumber is present, email format is valid if provided, and officer exists.
 */
@Component
public class UpdateOfficerCommandValidator extends CommandValidator {

    private final OfficerExistenceService officerExistenceService;

    public UpdateOfficerCommandValidator(OfficerExistenceService officerExistenceService) {
        this.officerExistenceService = officerExistenceService;
    }

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

        ValidationResult result = builder.build();
        if (!result.isValid()) {
            return result;
        }

        // Check if officer exists
        if (!officerExistenceService.exists(updateCommand.getBadgeNumber())) {
            throw new NotFoundException("Officer not found: " + updateCommand.getBadgeNumber());
        }

        return result;
    }
}
