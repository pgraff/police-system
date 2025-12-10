package com.knowit.policesystem.edge.validation.involvedparties;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.involvedparties.EndPartyInvolvementCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for EndPartyInvolvementCommand.
 * Ensures involvementId and involvementEndTime are provided.
 */
@Component
public class EndPartyInvolvementCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof EndPartyInvolvementCommand)) {
            return ValidationResult.valid();
        }

        EndPartyInvolvementCommand endCommand = (EndPartyInvolvementCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (endCommand.getInvolvementId() == null || endCommand.getInvolvementId().trim().isEmpty()) {
            builder.addError(new ValidationError("involvementId", "involvementId is required", endCommand.getInvolvementId()));
        }

        if (endCommand.getInvolvementEndTime() == null) {
            builder.addError(new ValidationError("involvementEndTime", "involvementEndTime is required", null));
        }

        return builder.build();
    }
}
