package com.knowit.policesystem.edge.validation.locations;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.locations.CreateLocationCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for CreateLocationCommand.
 * Validates that all required fields are present and coordinate ranges are valid.
 */
@Component
public class CreateLocationCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof CreateLocationCommand)) {
            return ValidationResult.valid();
        }

        CreateLocationCommand createCommand = (CreateLocationCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate locationId
        if (createCommand.getLocationId() == null || createCommand.getLocationId().trim().isEmpty()) {
            builder.addError(new ValidationError("locationId", "locationId is required", createCommand.getLocationId()));
        }

        // Validate latitude range if provided
        if (createCommand.getLatitude() != null) {
            double latitude = createCommand.getLatitude();
            if (latitude < -90.0 || latitude > 90.0) {
                builder.addError(new ValidationError("latitude", "latitude must be between -90.0 and 90.0", latitude));
            }
        }

        // Validate longitude range if provided
        if (createCommand.getLongitude() != null) {
            double longitude = createCommand.getLongitude();
            if (longitude < -180.0 || longitude > 180.0) {
                builder.addError(new ValidationError("longitude", "longitude must be between -180.0 and 180.0", longitude));
            }
        }

        // Note: locationType enum validation is handled by Jackson deserialization
        // If an invalid enum value is provided, Jackson will throw an exception before reaching this validator

        return builder.build();
    }
}
