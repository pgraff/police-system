package com.knowit.policesystem.edge.validation.involvedparties;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.involvedparties.InvolvePartyCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for InvolvePartyCommand.
 * Validates that all required fields are present, enum values are valid, and exactly one of incidentId, callId, or activityId is provided.
 */
@Component
public class InvolvePartyCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof InvolvePartyCommand)) {
            return ValidationResult.valid();
        }

        InvolvePartyCommand involveCommand = (InvolvePartyCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate involvementId
        if (involveCommand.getInvolvementId() == null || involveCommand.getInvolvementId().trim().isEmpty()) {
            builder.addError(new ValidationError("involvementId", "involvementId is required", involveCommand.getInvolvementId()));
        }

        // Validate personId
        if (involveCommand.getPersonId() == null || involveCommand.getPersonId().trim().isEmpty()) {
            builder.addError(new ValidationError("personId", "personId is required", involveCommand.getPersonId()));
        }

        // Validate partyRoleType enum
        if (involveCommand.getPartyRoleType() == null) {
            builder.addError(new ValidationError("partyRoleType", "partyRoleType is required", null));
        }

        // Validate XOR: exactly one of incidentId, callId, or activityId must be present
        boolean hasIncidentId = involveCommand.getIncidentId() != null && !involveCommand.getIncidentId().trim().isEmpty();
        boolean hasCallId = involveCommand.getCallId() != null && !involveCommand.getCallId().trim().isEmpty();
        boolean hasActivityId = involveCommand.getActivityId() != null && !involveCommand.getActivityId().trim().isEmpty();

        int targetCount = (hasIncidentId ? 1 : 0) + (hasCallId ? 1 : 0) + (hasActivityId ? 1 : 0);

        if (targetCount == 0) {
            builder.addError(new ValidationError("incidentId, callId, activityId", "Exactly one of incidentId, callId, or activityId must be provided", null));
        } else if (targetCount > 1) {
            builder.addError(new ValidationError("incidentId, callId, activityId", "Exactly one of incidentId, callId, or activityId must be provided, not multiple", null));
        }

        return builder.build();
    }
}
