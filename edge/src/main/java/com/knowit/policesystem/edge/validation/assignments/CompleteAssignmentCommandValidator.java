package com.knowit.policesystem.edge.validation.assignments;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.assignments.CompleteAssignmentCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for CompleteAssignmentCommand.
 */
@Component
public class CompleteAssignmentCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof CompleteAssignmentCommand)) {
            return ValidationResult.valid();
        }

        CompleteAssignmentCommand completeCommand = (CompleteAssignmentCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (completeCommand.getAssignmentId() == null || completeCommand.getAssignmentId().trim().isEmpty()) {
            builder.addError(new ValidationError("assignmentId", "assignmentId is required", completeCommand.getAssignmentId()));
        }

        if (completeCommand.getCompletedTime() == null) {
            builder.addError(new ValidationError("completedTime", "completedTime is required", null));
        }

        return builder.build();
    }
}
