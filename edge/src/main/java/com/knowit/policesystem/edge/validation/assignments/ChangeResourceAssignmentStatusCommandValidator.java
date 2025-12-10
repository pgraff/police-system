package com.knowit.policesystem.edge.validation.assignments;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.assignments.ChangeResourceAssignmentStatusCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ChangeResourceAssignmentStatusCommand.
 */
@Component
public class ChangeResourceAssignmentStatusCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ChangeResourceAssignmentStatusCommand)) {
            return ValidationResult.valid();
        }

        ChangeResourceAssignmentStatusCommand changeStatusCommand = (ChangeResourceAssignmentStatusCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (changeStatusCommand.getAssignmentId() == null || changeStatusCommand.getAssignmentId().trim().isEmpty()) {
            builder.addError(new ValidationError("assignmentId", "assignmentId is required", changeStatusCommand.getAssignmentId()));
        }

        if (changeStatusCommand.getResourceId() == null || changeStatusCommand.getResourceId().trim().isEmpty()) {
            builder.addError(new ValidationError("resourceId", "resourceId is required", changeStatusCommand.getResourceId()));
        }

        if (changeStatusCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        return builder.build();
    }
}
