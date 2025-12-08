package com.knowit.policesystem.edge.validation.units;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.units.UpdateUnitCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for UpdateUnitCommand.
 * Validates that unitId is present.
 * Enum validation is handled at the DTO level by Jackson deserialization.
 */
@Component
public class UpdateUnitCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof UpdateUnitCommand)) {
            return ValidationResult.valid();
        }

        UpdateUnitCommand updateCommand = (UpdateUnitCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate unitId
        if (updateCommand.getUnitId() == null || updateCommand.getUnitId().trim().isEmpty()) {
            builder.addError(new ValidationError("unitId", "unitId is required", updateCommand.getUnitId()));
        }

        // Note: Enum validation is handled at DTO level by Jackson deserialization
        // Invalid enum values will result in HttpMessageNotReadableException handled by GlobalExceptionHandler

        return builder.build();
    }
}
