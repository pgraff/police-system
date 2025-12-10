package com.knowit.policesystem.edge.validation.shifts;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.shifts.EndShiftCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for EndShiftCommand.
 * Ensures shiftId and endTime are provided.
 */
@Component
public class EndShiftCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof EndShiftCommand)) {
            return ValidationResult.valid();
        }

        EndShiftCommand endCommand = (EndShiftCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (endCommand.getShiftId() == null || endCommand.getShiftId().trim().isEmpty()) {
            builder.addError(new ValidationError("shiftId", "shiftId is required", endCommand.getShiftId()));
        }

        if (endCommand.getEndTime() == null) {
            builder.addError(new ValidationError("endTime", "endTime is required", null));
        }

        return builder.build();
    }
}
