package com.knowit.policesystem.edge.validation.shifts;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.shifts.CheckInOfficerCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for CheckInOfficerCommand.
 * Ensures shiftId, badgeNumber, checkInTime, and shiftRoleType are provided.
 */
@Component
public class CheckInOfficerCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof CheckInOfficerCommand)) {
            return ValidationResult.valid();
        }

        CheckInOfficerCommand checkInCommand = (CheckInOfficerCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (checkInCommand.getShiftId() == null || checkInCommand.getShiftId().trim().isEmpty()) {
            builder.addError(new ValidationError("shiftId", "shiftId is required", checkInCommand.getShiftId()));
        }

        if (checkInCommand.getBadgeNumber() == null || checkInCommand.getBadgeNumber().trim().isEmpty()) {
            builder.addError(new ValidationError("badgeNumber", "badgeNumber is required", checkInCommand.getBadgeNumber()));
        }

        if (checkInCommand.getCheckInTime() == null) {
            builder.addError(new ValidationError("checkInTime", "checkInTime is required", null));
        }

        if (checkInCommand.getShiftRoleType() == null) {
            builder.addError(new ValidationError("shiftRoleType", "shiftRoleType is required", null));
        }

        return builder.build();
    }
}
