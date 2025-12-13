package com.knowit.policesystem.edge.validation.activities;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.activities.ChangeActivityStatusCommand;
import com.knowit.policesystem.edge.exceptions.NotFoundException;
import com.knowit.policesystem.edge.services.activities.ActivityExistenceService;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ChangeActivityStatusCommand.
 * Validates that activityId is present, status is present, and activity exists.
 */
@Component
public class ChangeActivityStatusCommandValidator extends CommandValidator {

    private final ActivityExistenceService activityExistenceService;

    public ChangeActivityStatusCommandValidator(ActivityExistenceService activityExistenceService) {
        this.activityExistenceService = activityExistenceService;
    }

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ChangeActivityStatusCommand)) {
            return ValidationResult.valid();
        }

        ChangeActivityStatusCommand changeStatusCommand = (ChangeActivityStatusCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (changeStatusCommand.getActivityId() == null || changeStatusCommand.getActivityId().trim().isEmpty()) {
            builder.addError(new ValidationError("activityId", "activityId is required", changeStatusCommand.getActivityId()));
        }

        if (changeStatusCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        ValidationResult result = builder.build();
        if (!result.isValid()) {
            return result;
        }

        // Check if activity exists
        if (!activityExistenceService.exists(changeStatusCommand.getActivityId())) {
            throw new NotFoundException("Activity not found: " + changeStatusCommand.getActivityId());
        }

        return result;
    }
}
