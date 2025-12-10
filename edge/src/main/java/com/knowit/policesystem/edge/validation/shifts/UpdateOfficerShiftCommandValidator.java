package com.knowit.policesystem.edge.validation.shifts;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.shifts.UpdateOfficerShiftCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for UpdateOfficerShiftCommand.
 * Ensures shiftId and badgeNumber are provided, and at least one field (shiftRoleType) is provided.
 */
@Component
public class UpdateOfficerShiftCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof UpdateOfficerShiftCommand)) {
            return ValidationResult.valid();
        }

        UpdateOfficerShiftCommand updateCommand = (UpdateOfficerShiftCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (updateCommand.getShiftId() == null || updateCommand.getShiftId().trim().isEmpty()) {
            builder.addError(new ValidationError("shiftId", "shiftId is required", updateCommand.getShiftId()));
        }

        if (updateCommand.getBadgeNumber() == null || updateCommand.getBadgeNumber().trim().isEmpty()) {
            builder.addError(new ValidationError("badgeNumber", "badgeNumber is required", updateCommand.getBadgeNumber()));
        }

        boolean hasShiftRoleType = updateCommand.getShiftRoleType() != null;

        if (!hasShiftRoleType) {
            builder.addError(new ValidationError("payload", "At least one of shiftRoleType is required", null));
        }

        return builder.build();
    }
}
