package com.knowit.policesystem.edge.validation.activities;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.activities.StartActivityCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for StartActivityCommand.
 * Validates that all required fields are present and enum values are valid.
 */
@Component
public class StartActivityCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof StartActivityCommand)) {
            return ValidationResult.valid();
        }

        StartActivityCommand startCommand = (StartActivityCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate activityId
        if (startCommand.getActivityId() == null || startCommand.getActivityId().trim().isEmpty()) {
            builder.addError(new ValidationError("activityId", "activityId is required", startCommand.getActivityId()));
        }

        // Validate activityType enum
        if (startCommand.getActivityType() == null) {
            builder.addError(new ValidationError("activityType", "activityType is required", null));
        }

        // Validate status enum
        if (startCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        return builder.build();
    }
}
