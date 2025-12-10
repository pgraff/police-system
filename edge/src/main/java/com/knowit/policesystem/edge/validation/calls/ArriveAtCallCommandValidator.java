package com.knowit.policesystem.edge.validation.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.calls.ArriveAtCallCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for ArriveAtCallCommand.
 */
@Component
public class ArriveAtCallCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof ArriveAtCallCommand)) {
            return ValidationResult.valid();
        }

        ArriveAtCallCommand arriveCommand = (ArriveAtCallCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (arriveCommand.getCallId() == null || arriveCommand.getCallId().trim().isEmpty()) {
            builder.addError(new ValidationError("callId", "callId is required", arriveCommand.getCallId()));
        }

        if (arriveCommand.getArrivedTime() == null) {
            builder.addError(new ValidationError("arrivedTime", "arrivedTime is required", null));
        }

        return builder.build();
    }
}
