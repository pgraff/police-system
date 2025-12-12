package com.knowit.policesystem.edge.validation.officers;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.officers.ChangeOfficerStatusCommand;
import com.knowit.policesystem.edge.domain.OfficerStatus;
import com.knowit.policesystem.edge.exceptions.NotFoundException;
import com.knowit.policesystem.edge.services.officers.OfficerExistenceService;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ChangeOfficerStatusCommand.
 * Validates that badgeNumber is present, status is a valid OfficerStatus enum value, and officer exists.
 */
@Component
public class ChangeOfficerStatusCommandValidator extends CommandValidator {

    private final OfficerExistenceService officerExistenceService;

    public ChangeOfficerStatusCommandValidator(OfficerExistenceService officerExistenceService) {
        this.officerExistenceService = officerExistenceService;
    }

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

        ValidationResult result = builder.build();
        if (!result.isValid()) {
            return result;
        }

        // Check if officer exists
        if (!officerExistenceService.exists(changeStatusCommand.getBadgeNumber())) {
            throw new NotFoundException("Officer not found: " + changeStatusCommand.getBadgeNumber());
        }

        return result;
    }
}
