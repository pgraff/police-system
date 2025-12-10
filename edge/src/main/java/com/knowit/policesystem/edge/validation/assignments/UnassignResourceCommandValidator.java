package com.knowit.policesystem.edge.validation.assignments;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.assignments.UnassignResourceCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for UnassignResourceCommand.
 * Validates that all required fields are present.
 */
@Component
public class UnassignResourceCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof UnassignResourceCommand)) {
            return ValidationResult.valid();
        }

        UnassignResourceCommand unassignCommand = (UnassignResourceCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate assignmentId
        if (unassignCommand.getAssignmentId() == null || unassignCommand.getAssignmentId().trim().isEmpty()) {
            builder.addError(new ValidationError("assignmentId", "assignmentId is required", unassignCommand.getAssignmentId()));
        }

        // Validate resourceId
        if (unassignCommand.getResourceId() == null || unassignCommand.getResourceId().trim().isEmpty()) {
            builder.addError(new ValidationError("resourceId", "resourceId is required", unassignCommand.getResourceId()));
        }

        // Validate endTime
        if (unassignCommand.getEndTime() == null) {
            builder.addError(new ValidationError("endTime", "endTime is required", null));
        }

        return builder.build();
    }
}
