package com.knowit.policesystem.edge.validation.assignments;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.assignments.ChangeAssignmentStatusCommand;
import com.knowit.policesystem.edge.exceptions.NotFoundException;
import com.knowit.policesystem.edge.services.assignments.AssignmentExistenceService;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ChangeAssignmentStatusCommand.
 * Validates required fields and checks assignment exists.
 */
@Component
public class ChangeAssignmentStatusCommandValidator extends CommandValidator {

    private final AssignmentExistenceService assignmentExistenceService;

    public ChangeAssignmentStatusCommandValidator(AssignmentExistenceService assignmentExistenceService) {
        this.assignmentExistenceService = assignmentExistenceService;
    }

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ChangeAssignmentStatusCommand)) {
            return ValidationResult.valid();
        }

        ChangeAssignmentStatusCommand changeStatusCommand = (ChangeAssignmentStatusCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (changeStatusCommand.getAssignmentId() == null || changeStatusCommand.getAssignmentId().trim().isEmpty()) {
            builder.addError(new ValidationError("assignmentId", "assignmentId is required", changeStatusCommand.getAssignmentId()));
        }

        if (changeStatusCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        ValidationResult result = builder.build();
        if (!result.isValid()) {
            return result;
        }

        // Check if assignment exists
        if (!assignmentExistenceService.exists(changeStatusCommand.getAssignmentId())) {
            throw new NotFoundException("Assignment not found: " + changeStatusCommand.getAssignmentId());
        }

        return result;
    }
}
