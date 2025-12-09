package com.knowit.policesystem.edge.validation.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.incidents.ClearIncidentCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ClearIncidentCommand.
 * Ensures required fields are present (validation-only, no 404 checks).
 */
@Component
public class ClearIncidentCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ClearIncidentCommand)) {
            return ValidationResult.valid();
        }

        ClearIncidentCommand clearCommand = (ClearIncidentCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (clearCommand.getIncidentId() == null || clearCommand.getIncidentId().trim().isEmpty()) {
            builder.addError(new ValidationError("incidentId", "incidentId is required", clearCommand.getIncidentId()));
        }

        if (clearCommand.getClearedTime() == null) {
            builder.addError(new ValidationError("clearedTime", "clearedTime is required", null));
        }

        return builder.build();
    }
}
