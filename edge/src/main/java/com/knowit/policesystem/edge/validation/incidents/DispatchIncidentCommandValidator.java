package com.knowit.policesystem.edge.validation.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.incidents.DispatchIncidentCommand;
import com.knowit.policesystem.edge.exceptions.NotFoundException;
import com.knowit.policesystem.edge.services.incidents.IncidentExistenceService;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for DispatchIncidentCommand.
 * Ensures required fields are present and incident exists.
 */
@Component
public class DispatchIncidentCommandValidator extends CommandValidator {

    private final IncidentExistenceService incidentExistenceService;

    public DispatchIncidentCommandValidator(IncidentExistenceService incidentExistenceService) {
        this.incidentExistenceService = incidentExistenceService;
    }

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof DispatchIncidentCommand)) {
            return ValidationResult.valid();
        }

        DispatchIncidentCommand dispatchCommand = (DispatchIncidentCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate incidentId (from path variable)
        if (dispatchCommand.getIncidentId() == null || dispatchCommand.getIncidentId().trim().isEmpty()) {
            builder.addError(new ValidationError("incidentId", "incidentId is required", dispatchCommand.getIncidentId()));
        }

        // Validate dispatchedTime
        if (dispatchCommand.getDispatchedTime() == null) {
            builder.addError(new ValidationError("dispatchedTime", "dispatchedTime is required", null));
        }

        ValidationResult result = builder.build();
        if (!result.isValid()) {
            return result;
        }

        // Check if incident exists
        if (!incidentExistenceService.exists(dispatchCommand.getIncidentId())) {
            throw new NotFoundException("Incident not found: " + dispatchCommand.getIncidentId());
        }

        return result;
    }
}
