package com.knowit.policesystem.edge.validation.locations;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.locations.UnlinkLocationFromCallCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for UnlinkLocationFromCallCommand.
 * Validates required path parameters.
 */
@Component
public class UnlinkLocationFromCallCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof UnlinkLocationFromCallCommand)) {
            return ValidationResult.valid();
        }

        UnlinkLocationFromCallCommand unlinkCommand = (UnlinkLocationFromCallCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (unlinkCommand.getCallId() == null || unlinkCommand.getCallId().trim().isEmpty()) {
            builder.addError(new ValidationError("callId", "callId is required", unlinkCommand.getCallId()));
        }

        if (unlinkCommand.getLocationId() == null || unlinkCommand.getLocationId().trim().isEmpty()) {
            builder.addError(new ValidationError("locationId", "locationId is required", unlinkCommand.getLocationId()));
        }

        return builder.build();
    }
}
