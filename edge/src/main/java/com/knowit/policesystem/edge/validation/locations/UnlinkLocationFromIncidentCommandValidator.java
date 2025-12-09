package com.knowit.policesystem.edge.validation.locations;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.locations.UnlinkLocationFromIncidentCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for UnlinkLocationFromIncidentCommand.
 * Validates that all required fields are present.
 */
@Component
public class UnlinkLocationFromIncidentCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof UnlinkLocationFromIncidentCommand)) {
            return ValidationResult.valid();
        }

        UnlinkLocationFromIncidentCommand unlinkCommand = (UnlinkLocationFromIncidentCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate incidentId
        if (unlinkCommand.getIncidentId() == null || unlinkCommand.getIncidentId().trim().isEmpty()) {
            builder.addError(new ValidationError("incidentId", "incidentId is required", unlinkCommand.getIncidentId()));
        }

        // Validate locationId
        if (unlinkCommand.getLocationId() == null || unlinkCommand.getLocationId().trim().isEmpty()) {
            builder.addError(new ValidationError("locationId", "locationId is required", unlinkCommand.getLocationId()));
        }

        return builder.build();
    }
}
