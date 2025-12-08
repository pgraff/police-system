package com.knowit.policesystem.edge.validation.units;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.units.CreateUnitCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for CreateUnitCommand.
 * Validates that all required fields are present and enum values are valid.
 */
@Component
public class CreateUnitCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof CreateUnitCommand)) {
            return ValidationResult.valid();
        }

        CreateUnitCommand createCommand = (CreateUnitCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate unitId
        if (createCommand.getUnitId() == null || createCommand.getUnitId().trim().isEmpty()) {
            builder.addError(new ValidationError("unitId", "unitId is required", createCommand.getUnitId()));
        }

        // Validate unitType enum
        if (createCommand.getUnitType() == null) {
            builder.addError(new ValidationError("unitType", "unitType is required", null));
        }

        // Validate status enum
        if (createCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        return builder.build();
    }
}
