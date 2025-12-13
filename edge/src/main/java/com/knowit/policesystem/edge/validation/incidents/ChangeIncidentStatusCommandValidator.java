package com.knowit.policesystem.edge.validation.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.incidents.ChangeIncidentStatusCommand;
import com.knowit.policesystem.edge.exceptions.NotFoundException;
import com.knowit.policesystem.edge.services.incidents.IncidentExistenceService;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ChangeIncidentStatusCommand.
 * Ensures required fields are present, status is provided, and incident exists.
 */
@Component
public class ChangeIncidentStatusCommandValidator extends CommandValidator {

    private final IncidentExistenceService incidentExistenceService;

    public ChangeIncidentStatusCommandValidator(IncidentExistenceService incidentExistenceService) {
        this.incidentExistenceService = incidentExistenceService;
    }

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ChangeIncidentStatusCommand)) {
            return ValidationResult.valid();
        }

        ChangeIncidentStatusCommand changeCommand = (ChangeIncidentStatusCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (changeCommand.getIncidentId() == null || changeCommand.getIncidentId().trim().isEmpty()) {
            builder.addError(new ValidationError("incidentId", "incidentId is required", changeCommand.getIncidentId()));
        }

        if (changeCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        ValidationResult result = builder.build();
        if (!result.isValid()) {
            return result;
        }

        // Check if incident exists
        if (!incidentExistenceService.exists(changeCommand.getIncidentId())) {
            throw new NotFoundException("Incident not found: " + changeCommand.getIncidentId());
        }

        return result;
    }
}
