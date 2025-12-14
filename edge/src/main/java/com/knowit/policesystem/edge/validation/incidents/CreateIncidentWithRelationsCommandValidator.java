package com.knowit.policesystem.edge.validation.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.incidents.CreateIncidentWithRelationsCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for CreateIncidentWithRelationsCommand.
 * Validates that all required fields are present and enum values are valid.
 */
@Component
public class CreateIncidentWithRelationsCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof CreateIncidentWithRelationsCommand)) {
            return ValidationResult.valid();
        }

        CreateIncidentWithRelationsCommand createCommand = (CreateIncidentWithRelationsCommand) command;
        var requestDto = createCommand.getRequestDto();
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate incidentId
        if (requestDto.getIncidentId() == null || requestDto.getIncidentId().trim().isEmpty()) {
            builder.addError(new ValidationError("incidentId", "incidentId is required", null));
        }

        // Validate priority enum
        if (requestDto.getPriority() == null) {
            builder.addError(new ValidationError("priority", "priority is required", null));
        }

        // Validate status enum
        if (requestDto.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        // Validate incidentType enum
        if (requestDto.getIncidentType() == null) {
            builder.addError(new ValidationError("incidentType", "incidentType is required", null));
        }

        return builder.build();
    }
}
