package com.knowit.policesystem.edge.validation.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.calls.LinkCallToDispatchCommand;
import com.knowit.policesystem.edge.exceptions.NotFoundException;
import com.knowit.policesystem.edge.services.calls.CallExistenceService;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for LinkCallToDispatchCommand.
 */
@Component
public class LinkCallToDispatchCommandValidator extends CommandValidator {

    private final CallExistenceService callExistenceService;

    /**
     * Creates a new link call to dispatch command validator.
     *
     * @param callExistenceService the service to check call existence
     */
    public LinkCallToDispatchCommandValidator(CallExistenceService callExistenceService) {
        this.callExistenceService = callExistenceService;
    }

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof LinkCallToDispatchCommand)) {
            return ValidationResult.valid();
        }

        LinkCallToDispatchCommand linkCommand = (LinkCallToDispatchCommand) command;
        ValidationResult.Builder builder = ValidationResult.builder();

        if (linkCommand.getCallId() == null || linkCommand.getCallId().trim().isEmpty()) {
            builder.addError(new ValidationError("callId", "callId is required", linkCommand.getCallId()));
        }

        if (linkCommand.getDispatchId() == null || linkCommand.getDispatchId().trim().isEmpty()) {
            builder.addError(new ValidationError("dispatchId", "dispatchId is required", linkCommand.getDispatchId()));
        }

        ValidationResult result = builder.build();
        if (!result.isValid()) {
            return result;
        }

        if (!callExistenceService.exists(linkCommand.getCallId())) {
            throw new NotFoundException("Call not found: " + linkCommand.getCallId());
        }

        return result;
    }
}
