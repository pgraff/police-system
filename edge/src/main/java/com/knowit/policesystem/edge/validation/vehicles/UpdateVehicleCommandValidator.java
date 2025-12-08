package com.knowit.policesystem.edge.validation.vehicles;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.vehicles.UpdateVehicleCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for UpdateVehicleCommand.
 * Validates that unitId is present and VIN format is valid if provided.
 */
@Component
public class UpdateVehicleCommandValidator extends CommandValidator {

    private static final String VIN_PATTERN = "^[A-HJ-NPR-Z0-9]{17}$";

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof UpdateVehicleCommand)) {
            return ValidationResult.valid();
        }

        UpdateVehicleCommand updateCommand = (UpdateVehicleCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate unitId
        if (updateCommand.getUnitId() == null || updateCommand.getUnitId().trim().isEmpty()) {
            builder.addError(new ValidationError("unitId", "unitId is required", updateCommand.getUnitId()));
        }

        // Validate VIN format if provided (17 characters, alphanumeric, excluding I, O, Q)
        if (updateCommand.getVin() != null && !updateCommand.getVin().trim().isEmpty()) {
            String vin = updateCommand.getVin().trim();
            if (vin.length() != 17) {
                builder.addError(new ValidationError("vin", "vin must be exactly 17 characters", vin));
            } else if (!vin.matches(VIN_PATTERN)) {
                builder.addError(new ValidationError("vin", "vin must contain only alphanumeric characters (A-Z, 0-9) excluding I, O, Q", vin));
            }
        }

        return builder.build();
    }
}
