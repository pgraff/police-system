package com.knowit.policesystem.edge.validation.shifts;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.shifts.StartShiftCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for StartShiftCommand.
 * Validates that all required fields are present and enum values are provided.
 */
@Component
public class StartShiftCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof StartShiftCommand)) {
            return ValidationResult.valid();
        }

        StartShiftCommand startCommand = (StartShiftCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate shiftId
        if (startCommand.getShiftId() == null || startCommand.getShiftId().trim().isEmpty()) {
            builder.addError(new ValidationError("shiftId", "shiftId is required", startCommand.getShiftId()));
        }

        // Validate shiftType enum
        if (startCommand.getShiftType() == null) {
            builder.addError(new ValidationError("shiftType", "shiftType is required", null));
        }

        // Validate status enum
        if (startCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        return builder.build();
    }
}
