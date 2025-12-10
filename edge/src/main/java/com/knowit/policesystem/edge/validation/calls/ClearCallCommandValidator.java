package com.knowit.policesystem.edge.validation.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.calls.ClearCallCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ClearCallCommand.
 */
@Component
public class ClearCallCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ClearCallCommand)) {
            return ValidationResult.valid();
        }

        ClearCallCommand clearCommand = (ClearCallCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (clearCommand.getCallId() == null || clearCommand.getCallId().trim().isEmpty()) {
            builder.addError(new ValidationError("callId", "callId is required", clearCommand.getCallId()));
        }

        if (clearCommand.getClearedTime() == null) {
            builder.addError(new ValidationError("clearedTime", "clearedTime is required", null));
        }

        return builder.build();
    }
}
