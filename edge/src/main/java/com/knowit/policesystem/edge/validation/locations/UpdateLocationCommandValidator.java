package com.knowit.policesystem.edge.validation.locations;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.locations.UpdateLocationCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for UpdateLocationCommand.
 * Validates that locationId is present and coordinate ranges are valid if provided.
 * Enum validation is handled by Jackson deserialization at the DTO level.
 */
@Component
public class UpdateLocationCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof UpdateLocationCommand)) {
            return ValidationResult.valid();
        }

        UpdateLocationCommand updateCommand = (UpdateLocationCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate locationId
        if (updateCommand.getLocationId() == null || updateCommand.getLocationId().trim().isEmpty()) {
            builder.addError(new ValidationError("locationId", "locationId is required", updateCommand.getLocationId()));
        }

        // Validate latitude range if provided
        if (updateCommand.getLatitude() != null) {
            double latitude = updateCommand.getLatitude();
            if (latitude < -90.0 || latitude > 90.0) {
                builder.addError(new ValidationError("latitude", "latitude must be between -90.0 and 90.0", latitude));
            }
        }

        // Validate longitude range if provided
        if (updateCommand.getLongitude() != null) {
            double longitude = updateCommand.getLongitude();
            if (longitude < -180.0 || longitude > 180.0) {
                builder.addError(new ValidationError("longitude", "longitude must be between -180.0 and 180.0", longitude));
            }
        }

        // Note: locationType enum validation is handled by Jackson deserialization
        // If an invalid enum value is provided, Jackson will throw an exception before reaching this validator

        return builder.build();
    }
}
