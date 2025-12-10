package com.knowit.policesystem.edge.validation.activities;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.activities.CompleteActivityCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for CompleteActivityCommand.
 */
@Component
public class CompleteActivityCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof CompleteActivityCommand)) {
            return ValidationResult.valid();
        }

        CompleteActivityCommand completeCommand = (CompleteActivityCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (completeCommand.getActivityId() == null || completeCommand.getActivityId().trim().isEmpty()) {
            builder.addError(new ValidationError("activityId", "activityId is required", completeCommand.getActivityId()));
        }

        if (completeCommand.getCompletedTime() == null) {
            builder.addError(new ValidationError("completedTime", "completedTime is required", null));
        }

        return builder.build();
    }
}
