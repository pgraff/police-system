package com.knowit.policesystem.edge.validation.vehicles;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.vehicles.ChangeVehicleStatusCommand;
import com.knowit.policesystem.edge.domain.VehicleStatus;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ChangeVehicleStatusCommand.
 * Validates that unitId is present and status is a valid VehicleStatus enum value.
 */
@Component
public class ChangeVehicleStatusCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ChangeVehicleStatusCommand)) {
            return ValidationResult.valid();
        }

        ChangeVehicleStatusCommand changeStatusCommand = (ChangeVehicleStatusCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate unitId
        if (changeStatusCommand.getUnitId() == null || changeStatusCommand.getUnitId().trim().isEmpty()) {
            builder.addError(new ValidationError("unitId", "unitId is required", changeStatusCommand.getUnitId()));
        }

        // Validate status is present
        if (changeStatusCommand.getStatus() == null || changeStatusCommand.getStatus().trim().isEmpty()) {
            builder.addError(new ValidationError("status", "status is required", changeStatusCommand.getStatus()));
        } else {
            // Validate status is a valid VehicleStatus enum value
            try {
                VehicleStatus.valueOf(changeStatusCommand.getStatus());
            } catch (IllegalArgumentException e) {
                builder.addError(new ValidationError("status", 
                    "status must be one of: Available, Assigned, InUse, Maintenance, OutOfService", 
                    changeStatusCommand.getStatus()));
            }
        }

        return builder.build();
    }
}
