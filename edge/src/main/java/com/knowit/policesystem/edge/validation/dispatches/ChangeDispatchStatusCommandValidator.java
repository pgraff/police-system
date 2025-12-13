package com.knowit.policesystem.edge.validation.dispatches;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.dispatches.ChangeDispatchStatusCommand;
import com.knowit.policesystem.edge.exceptions.NotFoundException;
import com.knowit.policesystem.edge.services.dispatches.DispatchExistenceService;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ChangeDispatchStatusCommand.
 * Validates that dispatchId is present, status is present, and dispatch exists.
 */
@Component
public class ChangeDispatchStatusCommandValidator extends CommandValidator {

    private final DispatchExistenceService dispatchExistenceService;

    public ChangeDispatchStatusCommandValidator(DispatchExistenceService dispatchExistenceService) {
        this.dispatchExistenceService = dispatchExistenceService;
    }

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ChangeDispatchStatusCommand)) {
            return ValidationResult.valid();
        }

        ChangeDispatchStatusCommand changeStatusCommand = (ChangeDispatchStatusCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (changeStatusCommand.getDispatchId() == null || changeStatusCommand.getDispatchId().trim().isEmpty()) {
            builder.addError(new ValidationError("dispatchId", "dispatchId is required", changeStatusCommand.getDispatchId()));
        }

        if (changeStatusCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        ValidationResult result = builder.build();
        if (!result.isValid()) {
            return result;
        }

        // Check if dispatch exists
        if (!dispatchExistenceService.exists(changeStatusCommand.getDispatchId())) {
            throw new NotFoundException("Dispatch not found: " + changeStatusCommand.getDispatchId());
        }

        return result;
    }
}
