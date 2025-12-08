package com.knowit.policesystem.edge.validation.locations;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.locations.LinkLocationToIncidentCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for LinkLocationToIncidentCommand.
 * Validates that all required fields are present.
 */
@Component
public class LinkLocationToIncidentCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof LinkLocationToIncidentCommand)) {
            return ValidationResult.valid();
        }

        LinkLocationToIncidentCommand linkCommand = (LinkLocationToIncidentCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate incidentId
        if (linkCommand.getIncidentId() == null || linkCommand.getIncidentId().trim().isEmpty()) {
            builder.addError(new ValidationError("incidentId", "incidentId is required", linkCommand.getIncidentId()));
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
