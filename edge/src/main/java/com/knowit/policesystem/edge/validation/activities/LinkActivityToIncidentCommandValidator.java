package com.knowit.policesystem.edge.validation.activities;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.activities.LinkActivityToIncidentCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for LinkActivityToIncidentCommand.
 */
@Component
public class LinkActivityToIncidentCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof LinkActivityToIncidentCommand)) {
            return ValidationResult.valid();
        }

        LinkActivityToIncidentCommand linkCommand = (LinkActivityToIncidentCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (linkCommand.getActivityId() == null || linkCommand.getActivityId().trim().isEmpty()) {
            builder.addError(new ValidationError("activityId", "activityId is required", linkCommand.getActivityId()));
        }

        if (linkCommand.getIncidentId() == null || linkCommand.getIncidentId().trim().isEmpty()) {
            builder.addError(new ValidationError("incidentId", "incidentId is required", linkCommand.getIncidentId()));
        }

        return builder.build();
    }
}
