package com.knowit.policesystem.edge.validation.activities;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.activities.UpdateActivityCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for UpdateActivityCommand.
 */
@Component
public class UpdateActivityCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof UpdateActivityCommand)) {
            return ValidationResult.valid();
        }

        UpdateActivityCommand updateCommand = (UpdateActivityCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (updateCommand.getActivityId() == null || updateCommand.getActivityId().trim().isEmpty()) {
            builder.addError(new ValidationError("activityId", "activityId is required", updateCommand.getActivityId()));
        }

        String description = updateCommand.getDescription();
        if (description != null && description.trim().isEmpty()) {
            builder.addError(new ValidationError("description", "description must not be blank", description));
        }

        return builder.build();
    }
}
