package com.knowit.policesystem.edge.validation.shifts;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.shifts.ChangeShiftStatusCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ChangeShiftStatusCommand.
 */
@Component
public class ChangeShiftStatusCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ChangeShiftStatusCommand)) {
            return ValidationResult.valid();
        }

        ChangeShiftStatusCommand changeStatusCommand = (ChangeShiftStatusCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (changeStatusCommand.getShiftId() == null || changeStatusCommand.getShiftId().trim().isEmpty()) {
            builder.addError(new ValidationError("shiftId", "shiftId is required", changeStatusCommand.getShiftId()));
        }

        if (changeStatusCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        return builder.build();
    }
}
