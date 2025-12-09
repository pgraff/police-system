package com.knowit.policesystem.edge.validation.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.incidents.ChangeIncidentStatusCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ChangeIncidentStatusCommand.
 * Ensures required fields are present and status is provided.
 */
@Component
public class ChangeIncidentStatusCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ChangeIncidentStatusCommand)) {
            return ValidationResult.valid();
        }

        ChangeIncidentStatusCommand changeCommand = (ChangeIncidentStatusCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (changeCommand.getIncidentId() == null || changeCommand.getIncidentId().trim().isEmpty()) {
            builder.addError(new ValidationError("incidentId", "incidentId is required", changeCommand.getIncidentId()));
        }

        if (changeCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        return builder.build();
    }
}
