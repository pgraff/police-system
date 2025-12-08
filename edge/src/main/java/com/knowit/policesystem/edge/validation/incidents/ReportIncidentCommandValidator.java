package com.knowit.policesystem.edge.validation.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.incidents.ReportIncidentCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ReportIncidentCommand.
 * Validates that all required fields are present and enum values are valid.
 */
@Component
public class ReportIncidentCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ReportIncidentCommand)) {
            return ValidationResult.valid();
        }

        ReportIncidentCommand reportCommand = (ReportIncidentCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate incidentId
        if (reportCommand.getIncidentId() == null || reportCommand.getIncidentId().trim().isEmpty()) {
            builder.addError(new ValidationError("incidentId", "incidentId is required", reportCommand.getIncidentId()));
        }

        // Validate priority enum
        if (reportCommand.getPriority() == null) {
            builder.addError(new ValidationError("priority", "priority is required", null));
        }

        // Validate status enum
        if (reportCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        // Validate incidentType enum
        if (reportCommand.getIncidentType() == null) {
            builder.addError(new ValidationError("incidentType", "incidentType is required", null));
        }

        return builder.build();
    }
}
