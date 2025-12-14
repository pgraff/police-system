package com.knowit.policesystem.edge.validation.officers;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.officers.BatchRegisterOfficersCommand;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for BatchRegisterOfficersCommand.
 * Validates that the officers list is not empty and each officer has required fields.
 */
@Component
public class BatchRegisterOfficersCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof BatchRegisterOfficersCommand)) {
            return ValidationResult.valid();
        }

        BatchRegisterOfficersCommand batchCommand = (BatchRegisterOfficersCommand) command;
        var requestDto = batchCommand.getRequestDto();
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate officers list is not empty
        if (requestDto.getOfficers() == null || requestDto.getOfficers().isEmpty()) {
            builder.addError(new ValidationError("officers", "officers list cannot be empty", null));
            return builder.build();
        }

        // Validate each officer (basic validation - detailed validation happens at DTO level)
        for (int i = 0; i < requestDto.getOfficers().size(); i++) {
            var officer = requestDto.getOfficers().get(i);
            String prefix = "officers[" + i + "]";
            
            if (officer.getBadgeNumber() == null || officer.getBadgeNumber().trim().isEmpty()) {
                builder.addError(new ValidationError(prefix + ".badgeNumber", prefix + ".badgeNumber is required", null));
            }
        }

        return builder.build();
    }
}
