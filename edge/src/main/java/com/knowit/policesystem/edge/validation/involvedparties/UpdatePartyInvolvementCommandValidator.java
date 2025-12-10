package com.knowit.policesystem.edge.validation.involvedparties;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.involvedparties.UpdatePartyInvolvementCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for UpdatePartyInvolvementCommand.
 * Ensures involvementId is provided and at least one field (partyRoleType or description) is provided.
 */
@Component
public class UpdatePartyInvolvementCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof UpdatePartyInvolvementCommand)) {
            return ValidationResult.valid();
        }

        UpdatePartyInvolvementCommand updateCommand = (UpdatePartyInvolvementCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (updateCommand.getInvolvementId() == null || updateCommand.getInvolvementId().trim().isEmpty()) {
            builder.addError(new ValidationError("involvementId", "involvementId is required", updateCommand.getInvolvementId()));
        }

        boolean hasPartyRoleType = updateCommand.getPartyRoleType() != null;
        boolean hasDescription = updateCommand.getDescription() != null && !updateCommand.getDescription().trim().isEmpty();

        if (!hasPartyRoleType && !hasDescription) {
            builder.addError(new ValidationError("payload", "At least one of partyRoleType or description is required", null));
        }

        if (updateCommand.getDescription() != null && updateCommand.getDescription().trim().isEmpty()) {
            builder.addError(new ValidationError("description", "description cannot be blank", updateCommand.getDescription()));
        }

        return builder.build();
    }
}
