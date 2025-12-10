package com.knowit.policesystem.edge.validation.activities;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.activities.ChangeActivityStatusCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ChangeActivityStatusCommand.
 */
@Component
public class ChangeActivityStatusCommandValidator extends CommandValidator {

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

        return builder.build();
    }
}
