package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.officers.RegisterOfficerCommand;
import com.knowit.policesystem.edge.dto.OfficerResponseDto;
import com.knowit.policesystem.edge.dto.RegisterOfficerRequestDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.officers.RegisterOfficerCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for officer operations.
 * Handles HTTP requests for officer-related endpoints.
 */
@RestController
public class OfficerController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final RegisterOfficerCommandValidator commandValidator;

    /**
     * Creates a new officer controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the command validator
     */
    public OfficerController(CommandHandlerRegistry commandHandlerRegistry,
                            RegisterOfficerCommandValidator commandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
    }

    /**
     * Registers a new officer.
     * Accepts officer data, validates it, and publishes a RegisterOfficerRequested event to Kafka and NATS/JetStream.
     *
     * @param requestDto the officer registration request DTO
     * @return 201 Created with officer ID and badge number
     */
    @PostMapping("/officers")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<OfficerResponseDto>> registerOfficer(
            @Valid @RequestBody RegisterOfficerRequestDto requestDto) {

        // Create command from DTO
        RegisterOfficerCommand command = new RegisterOfficerCommand(requestDto.getBadgeNumber(), requestDto);

        // Validate command
        var validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<RegisterOfficerCommand, OfficerResponseDto> handler =
                commandHandlerRegistry.findHandler(RegisterOfficerCommand.class);
        OfficerResponseDto response = handler.handle(command);

        // Return 201 Created response
        return created(response, "Officer registration request created");
    }
}
