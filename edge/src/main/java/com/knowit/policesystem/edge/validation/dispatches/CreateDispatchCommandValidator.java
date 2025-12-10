package com.knowit.policesystem.edge.validation.dispatches;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.dispatches.CreateDispatchCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for CreateDispatchCommand.
 * Validates that all required fields are present and enum values are valid.
 */
@Component
public class CreateDispatchCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof CreateDispatchCommand)) {
            return ValidationResult.valid();
        }

        CreateDispatchCommand createCommand = (CreateDispatchCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate dispatchId
        if (createCommand.getDispatchId() == null || createCommand.getDispatchId().trim().isEmpty()) {
            builder.addError(new ValidationError("dispatchId", "dispatchId is required", createCommand.getDispatchId()));
        }

        // Validate dispatchTime
        if (createCommand.getDispatchTime() == null) {
            builder.addError(new ValidationError("dispatchTime", "dispatchTime is required", null));
        }

        // Validate dispatchType enum
        if (createCommand.getDispatchType() == null) {
            builder.addError(new ValidationError("dispatchType", "dispatchType is required", null));
        }

        // Validate status enum
        if (createCommand.getStatus() == null) {
            builder.addError(new ValidationError("status", "status is required", null));
        }

        return builder.build();
    }
}
