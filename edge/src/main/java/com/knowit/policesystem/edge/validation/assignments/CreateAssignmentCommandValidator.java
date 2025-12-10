package com.knowit.policesystem.edge.validation.assignments;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.assignments.CreateAssignmentCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for CreateAssignmentCommand.
 * Validates that all required fields are present, enum values are valid, and exactly one of incidentId or callId is provided.
 */
@Component
public class CreateAssignmentCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof CreateAssignmentCommand)) {
            return ValidationResult.valid();
        }

        CreateAssignmentCommand createCommand = (CreateAssignmentCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate assignmentId
        if (createCommand.getAssignmentId() == null || createCommand.getAssignmentId().trim().isEmpty()) {
            builder.addError(new ValidationError("assignmentId", "assignmentId is required", createCommand.getAssignmentId()));
        }

        // Validate assignmentType enum
        if (createCommand.getAssignmentType() == null) {
            builder.addError(new ValidationError("assignmentType", "assignmentType is required", null));
        }

        // Validate status enum
        if (createCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        // Validate XOR: exactly one of incidentId or callId must be present
        boolean hasIncidentId = createCommand.getIncidentId() != null && !createCommand.getIncidentId().trim().isEmpty();
        boolean hasCallId = createCommand.getCallId() != null && !createCommand.getCallId().trim().isEmpty();

        if (hasIncidentId && hasCallId) {
            builder.addError(new ValidationError("incidentId, callId", "Exactly one of incidentId or callId must be provided, not both", null));
        } else if (!hasIncidentId && !hasCallId) {
            builder.addError(new ValidationError("incidentId, callId", "Exactly one of incidentId or callId must be provided", null));
        }

        return builder.build();
    }
}
