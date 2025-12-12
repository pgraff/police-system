package com.knowit.policesystem.edge.validation.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.incidents.UpdateIncidentCommand;
import com.knowit.policesystem.edge.exceptions.NotFoundException;
import com.knowit.policesystem.edge.services.incidents.IncidentExistenceService;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for UpdateIncidentCommand.
 * Ensures required fields are present, validates enum values, and checks incident exists.
 */
@Component
public class UpdateIncidentCommandValidator extends CommandValidator {

    private final IncidentExistenceService incidentExistenceService;

    public UpdateIncidentCommandValidator(IncidentExistenceService incidentExistenceService) {
        this.incidentExistenceService = incidentExistenceService;
    }

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof UpdateIncidentCommand)) {
            return ValidationResult.valid();
        }

        UpdateIncidentCommand updateCommand = (UpdateIncidentCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (updateCommand.getIncidentId() == null || updateCommand.getIncidentId().trim().isEmpty()) {
            builder.addError(new ValidationError("incidentId", "incidentId is required", updateCommand.getIncidentId()));
        }

        ValidationResult result = builder.build();
        if (!result.isValid()) {
            return result;
        }

        // Check if incident exists
        if (!incidentExistenceService.exists(updateCommand.getIncidentId())) {
            throw new NotFoundException("Incident not found: " + updateCommand.getIncidentId());
        }

        // All fields are optional, but if provided, they should be valid
        // Enum validation is handled by Jackson deserialization, but we can add additional checks if needed

        return result;
    }
}
