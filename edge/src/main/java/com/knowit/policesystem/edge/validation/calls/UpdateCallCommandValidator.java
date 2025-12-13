package com.knowit.policesystem.edge.validation.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.calls.UpdateCallCommand;
import com.knowit.policesystem.edge.exceptions.NotFoundException;
import com.knowit.policesystem.edge.services.calls.CallExistenceService;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for UpdateCallCommand.
 * Validates that at least one field is provided and that the call exists.
 */
@Component
public class UpdateCallCommandValidator extends CommandValidator {

    private final CallExistenceService callExistenceService;

    public UpdateCallCommandValidator(CallExistenceService callExistenceService) {
        this.callExistenceService = callExistenceService;
    }

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof UpdateCallCommand)) {
            return ValidationResult.valid();
        }

        UpdateCallCommand updateCommand = (UpdateCallCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (updateCommand.getCallId() == null || updateCommand.getCallId().trim().isEmpty()) {
            builder.addError(new ValidationError("callId", "callId is required", updateCommand.getCallId()));
        }

        boolean hasPriority = updateCommand.getPriority() != null;
        boolean hasDescription = updateCommand.getDescription() != null && !updateCommand.getDescription().trim().isEmpty();
        boolean hasCallType = updateCommand.getCallType() != null;

        if (!hasPriority && !hasDescription && !hasCallType) {
            builder.addError(new ValidationError("payload", "At least one of priority, description, or callType is required", null));
        }

        if (updateCommand.getDescription() != null && updateCommand.getDescription().trim().isEmpty()) {
            builder.addError(new ValidationError("description", "description cannot be blank", updateCommand.getDescription()));
        }

        ValidationResult result = builder.build();
        if (!result.isValid()) {
            return result;
        }

        // Check if call exists
        if (!callExistenceService.exists(updateCommand.getCallId())) {
            throw new NotFoundException("Call not found: " + updateCommand.getCallId());
        }

        return result;
    }
}
