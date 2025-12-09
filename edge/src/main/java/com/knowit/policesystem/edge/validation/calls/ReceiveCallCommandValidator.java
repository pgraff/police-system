package com.knowit.policesystem.edge.validation.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.calls.ReceiveCallCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ReceiveCallCommand.
 * Validates that all required fields are present and enum values are valid.
 */
@Component
public class ReceiveCallCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ReceiveCallCommand)) {
            return ValidationResult.valid();
        }

        ReceiveCallCommand receiveCommand = (ReceiveCallCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate callId
        if (receiveCommand.getCallId() == null || receiveCommand.getCallId().trim().isEmpty()) {
            builder.addError(new ValidationError("callId", "callId is required", receiveCommand.getCallId()));
        }

        // Validate priority enum
        if (receiveCommand.getPriority() == null) {
            builder.addError(new ValidationError("priority", "priority is required", null));
        }

        // Validate status enum
        if (receiveCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        // Validate callType enum
        if (receiveCommand.getCallType() == null) {
            builder.addError(new ValidationError("callType", "callType is required", null));
        }

        return builder.build();
    }
}
