package com.knowit.policesystem.edge.validation.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.calls.DispatchCallCommand;
import com.knowit.policesystem.edge.exceptions.NotFoundException;
import com.knowit.policesystem.edge.services.calls.CallExistenceService;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for DispatchCallCommand.
 */
@Component
public class DispatchCallCommandValidator extends CommandValidator {

    private final CallExistenceService callExistenceService;

    public DispatchCallCommandValidator(CallExistenceService callExistenceService) {
        this.callExistenceService = callExistenceService;
    }

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof DispatchCallCommand)) {
            return ValidationResult.valid();
        }

        DispatchCallCommand dispatchCommand = (DispatchCallCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (dispatchCommand.getCallId() == null || dispatchCommand.getCallId().trim().isEmpty()) {
            builder.addError(new ValidationError("callId", "callId is required", dispatchCommand.getCallId()));
        }

        if (dispatchCommand.getDispatchedTime() == null) {
            builder.addError(new ValidationError("dispatchedTime", "dispatchedTime is required", null));
        }

        ValidationResult result = builder.build();
        if (!result.isValid()) {
            return result;
        }

        if (!callExistenceService.exists(dispatchCommand.getCallId())) {
            throw new NotFoundException("Call not found: " + dispatchCommand.getCallId());
        }

        return result;
    }
}
