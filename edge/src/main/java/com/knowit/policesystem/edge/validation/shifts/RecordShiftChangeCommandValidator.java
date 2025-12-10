package com.knowit.policesystem.edge.validation.shifts;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.shifts.RecordShiftChangeCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for RecordShiftChangeCommand.
 */
@Component
public class RecordShiftChangeCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof RecordShiftChangeCommand)) {
            return ValidationResult.valid();
        }

        RecordShiftChangeCommand recordShiftChangeCommand = (RecordShiftChangeCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (recordShiftChangeCommand.getShiftId() == null || recordShiftChangeCommand.getShiftId().trim().isEmpty()) {
            builder.addError(new ValidationError("shiftId", "shiftId is required", recordShiftChangeCommand.getShiftId()));
        }

        if (recordShiftChangeCommand.getShiftChangeId() == null || recordShiftChangeCommand.getShiftChangeId().trim().isEmpty()) {
            builder.addError(new ValidationError("shiftChangeId", "shiftChangeId is required", recordShiftChangeCommand.getShiftChangeId()));
        }

        if (recordShiftChangeCommand.getChangeType() == null) {
            builder.addError(new ValidationError("changeType", "changeType is required", null));
        }

        return builder.build();
    }
}
