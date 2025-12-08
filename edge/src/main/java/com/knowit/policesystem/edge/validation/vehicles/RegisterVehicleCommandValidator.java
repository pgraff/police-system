package com.knowit.policesystem.edge.validation.vehicles;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.vehicles.RegisterVehicleCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for RegisterVehicleCommand.
 * Validates that all required fields are present, VIN format is correct, and enum values are valid.
 */
@Component
public class RegisterVehicleCommandValidator extends CommandValidator {

    private static final String VIN_PATTERN = "^[A-HJ-NPR-Z0-9]{17}$";

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof RegisterVehicleCommand)) {
            return ValidationResult.valid();
        }

        RegisterVehicleCommand registerCommand = (RegisterVehicleCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate unitId
        if (registerCommand.getUnitId() == null || registerCommand.getUnitId().trim().isEmpty()) {
            builder.addError(new ValidationError("unitId", "unitId is required", registerCommand.getUnitId()));
        }

        // Validate VIN format (17 characters, alphanumeric, excluding I, O, Q)
        if (registerCommand.getVin() != null && !registerCommand.getVin().trim().isEmpty()) {
            String vin = registerCommand.getVin().trim();
            if (vin.length() != 17) {
                builder.addError(new ValidationError("vin", "vin must be exactly 17 characters", vin));
            } else if (!vin.matches(VIN_PATTERN)) {
                builder.addError(new ValidationError("vin", "vin must contain only alphanumeric characters (A-Z, 0-9) excluding I, O, Q", vin));
            }
        } else {
            builder.addError(new ValidationError("vin", "vin is required", null));
        }

        // Validate status enum
        if (registerCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        // Validate vehicleType enum
        if (registerCommand.getVehicleType() == null) {
            builder.addError(new ValidationError("vehicleType", "vehicleType is required", null));
        }

        return builder.build();
    }
}
