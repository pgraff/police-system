package com.knowit.policesystem.edge.validation.assignments;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.assignments.LinkAssignmentToDispatchCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for LinkAssignmentToDispatchCommand.
 */
@Component
public class LinkAssignmentToDispatchCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof LinkAssignmentToDispatchCommand)) {
            return ValidationResult.valid();
        }

        LinkAssignmentToDispatchCommand linkCommand = (LinkAssignmentToDispatchCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (linkCommand.getAssignmentId() == null || linkCommand.getAssignmentId().trim().isEmpty()) {
            builder.addError(new ValidationError("assignmentId", "assignmentId is required", linkCommand.getAssignmentId()));
        }

        if (linkCommand.getDispatchId() == null || linkCommand.getDispatchId().trim().isEmpty()) {
            builder.addError(new ValidationError("dispatchId", "dispatchId is required", linkCommand.getDispatchId()));
        }

        return builder.build();
    }
}
