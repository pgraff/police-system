package com.knowit.policesystem.edge.validation.locations;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.locations.LinkLocationToCallCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for LinkLocationToCallCommand.
 * Validates that all required fields are present.
 */
@Component
public class LinkLocationToCallCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof LinkLocationToCallCommand)) {
            return ValidationResult.valid();
        }

        LinkLocationToCallCommand linkCommand = (LinkLocationToCallCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate callId
        if (linkCommand.getCallId() == null || linkCommand.getCallId().trim().isEmpty()) {
            builder.addError(new ValidationError("callId", "callId is required", linkCommand.getCallId()));
        }

        // Validate locationId
        if (linkCommand.getLocationId() == null || linkCommand.getLocationId().trim().isEmpty()) {
            builder.addError(new ValidationError("locationId", "locationId is required", linkCommand.getLocationId()));
        }

        // Validate locationRoleType
        if (linkCommand.getLocationRoleType() == null) {
            builder.addError(new ValidationError("locationRoleType", "locationRoleType is required", null));
        }

        // Note: locationRoleType enum validation is handled by Jackson deserialization
        // If an invalid enum value is provided, Jackson will throw an exception before reaching this validator

        return builder.build();
    }
}
