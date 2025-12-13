package com.knowit.policesystem.edge.validation.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.calls.ChangeCallStatusCommand;
import com.knowit.policesystem.edge.exceptions.NotFoundException;
import com.knowit.policesystem.edge.services.calls.CallExistenceService;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ChangeCallStatusCommand.
 * Validates that callId is present, status is present, and call exists.
 */
@Component
public class ChangeCallStatusCommandValidator extends CommandValidator {

    private final CallExistenceService callExistenceService;

    public ChangeCallStatusCommandValidator(CallExistenceService callExistenceService) {
        this.callExistenceService = callExistenceService;
    }

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ChangeCallStatusCommand)) {
            return ValidationResult.valid();
        }

        ChangeCallStatusCommand changeStatusCommand = (ChangeCallStatusCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (changeStatusCommand.getCallId() == null || changeStatusCommand.getCallId().trim().isEmpty()) {
            builder.addError(new ValidationError("callId", "callId is required", changeStatusCommand.getCallId()));
        }

        if (changeStatusCommand.getStatus() == null || changeStatusCommand.getStatus().trim().isEmpty()) {
            builder.addError(new ValidationError("status", "status is required", changeStatusCommand.getStatus()));
        }

        ValidationResult result = builder.build();
        if (!result.isValid()) {
            return result;
        }

        // Check if call exists
        if (!callExistenceService.exists(changeStatusCommand.getCallId())) {
            throw new NotFoundException("Call not found: " + changeStatusCommand.getCallId());
        }

        return result;
    }
}
