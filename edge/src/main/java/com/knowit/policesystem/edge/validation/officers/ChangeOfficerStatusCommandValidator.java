package com.knowit.policesystem.edge.validation.officers;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.officers.ChangeOfficerStatusCommand;
import com.knowit.policesystem.edge.domain.OfficerStatus;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ChangeOfficerStatusCommand.
 * Validates that badgeNumber is present and status is a valid OfficerStatus enum value.
 */
@Component
public class ChangeOfficerStatusCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ChangeOfficerStatusCommand)) {
            return ValidationResult.valid();
        }

        ChangeOfficerStatusCommand changeStatusCommand = (ChangeOfficerStatusCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate badgeNumber
        if (changeStatusCommand.getBadgeNumber() == null || changeStatusCommand.getBadgeNumber().trim().isEmpty()) {
            builder.addError(new ValidationError("badgeNumber", "badgeNumber is required", changeStatusCommand.getBadgeNumber()));
        }

        // Validate status is present
        if (changeStatusCommand.getStatus() == null || changeStatusCommand.getStatus().trim().isEmpty()) {
            builder.addError(new ValidationError("status", "status is required", changeStatusCommand.getStatus()));
        } else {
            // Validate status is a valid OfficerStatus enum value
            try {
                OfficerStatus.valueOf(changeStatusCommand.getStatus());
            } catch (IllegalArgumentException e) {
                builder.addError(new ValidationError("status", 
                    "status must be one of: Active, OnDuty, OffDuty, Suspended, Retired", 
                    changeStatusCommand.getStatus()));
            }
        }

        return builder.build();
    }
}
