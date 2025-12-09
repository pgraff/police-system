package com.knowit.policesystem.edge.validation.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.incidents.ArriveAtIncidentCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ArriveAtIncidentCommand.
 * Ensures required fields are present (validation-only, no 404 checks).
 */
@Component
public class ArriveAtIncidentCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ArriveAtIncidentCommand)) {
            return ValidationResult.valid();
        }

        ArriveAtIncidentCommand arriveCommand = (ArriveAtIncidentCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (arriveCommand.getIncidentId() == null || arriveCommand.getIncidentId().trim().isEmpty()) {
            builder.addError(new ValidationError("incidentId", "incidentId is required", arriveCommand.getIncidentId()));
        }

        if (arriveCommand.getArrivedTime() == null) {
            builder.addError(new ValidationError("arrivedTime", "arrivedTime is required", null));
        }

        return builder.build();
    }
}
