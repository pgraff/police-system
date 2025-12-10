package com.knowit.policesystem.edge.validation.assignments;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.assignments.AssignResourceCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for AssignResourceCommand.
 * Validates that all required fields are present and enum values are valid.
 */
@Component
public class AssignResourceCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof AssignResourceCommand)) {
            return ValidationResult.valid();
        }

        AssignResourceCommand assignCommand = (AssignResourceCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate assignmentId
        if (assignCommand.getAssignmentId() == null || assignCommand.getAssignmentId().trim().isEmpty()) {
            builder.addError(new ValidationError("assignmentId", "assignmentId is required", assignCommand.getAssignmentId()));
        }

        // Validate resourceId
        if (assignCommand.getResourceId() == null || assignCommand.getResourceId().trim().isEmpty()) {
            builder.addError(new ValidationError("resourceId", "resourceId is required", assignCommand.getResourceId()));
        }

        // Validate resourceType enum
        if (assignCommand.getResourceType() == null) {
            builder.addError(new ValidationError("resourceType", "resourceType is required", null));
        }
        // Note: resourceType enum validation is handled by Jackson deserialization

        // Validate roleType enum
        if (assignCommand.getRoleType() == null) {
            builder.addError(new ValidationError("roleType", "roleType is required", null));
        }
        // Note: roleType enum validation is handled by Jackson deserialization

        // Validate status
        if (assignCommand.getStatus() == null || assignCommand.getStatus().trim().isEmpty()) {
            builder.addError(new ValidationError("status", "status is required", assignCommand.getStatus()));
        }

        return builder.build();
    }
}
