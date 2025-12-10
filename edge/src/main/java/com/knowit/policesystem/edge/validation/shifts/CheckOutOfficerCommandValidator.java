package com.knowit.policesystem.edge.validation.shifts;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.shifts.CheckOutOfficerCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for CheckOutOfficerCommand.
 * Ensures shiftId, badgeNumber, and checkOutTime are provided.
 */
@Component
public class CheckOutOfficerCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof CheckOutOfficerCommand)) {
            return ValidationResult.valid();
        }

        CheckOutOfficerCommand checkOutCommand = (CheckOutOfficerCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (checkOutCommand.getShiftId() == null || checkOutCommand.getShiftId().trim().isEmpty()) {
            builder.addError(new ValidationError("shiftId", "shiftId is required", checkOutCommand.getShiftId()));
        }

        if (checkOutCommand.getBadgeNumber() == null || checkOutCommand.getBadgeNumber().trim().isEmpty()) {
            builder.addError(new ValidationError("badgeNumber", "badgeNumber is required", checkOutCommand.getBadgeNumber()));
        }

        if (checkOutCommand.getCheckOutTime() == null) {
            builder.addError(new ValidationError("checkOutTime", "checkOutTime is required", null));
        }

        return builder.build();
    }
}
