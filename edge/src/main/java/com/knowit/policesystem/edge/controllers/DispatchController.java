package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.dispatches.CreateDispatchCommand;
import com.knowit.policesystem.edge.dto.CreateDispatchRequestDto;
import com.knowit.policesystem.edge.dto.DispatchResponseDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.dispatches.CreateDispatchCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for dispatch operations.
 * Handles HTTP requests for dispatch-related endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class DispatchController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final CreateDispatchCommandValidator commandValidator;

    /**
     * Creates a new dispatch controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the create dispatch command validator
     */
    public DispatchController(CommandHandlerRegistry commandHandlerRegistry,
                              CreateDispatchCommandValidator commandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
    }

    /**
     * Creates a new dispatch.
     * Accepts dispatch data, validates it, and publishes a CreateDispatchRequested event to Kafka.
     *
     * @param requestDto the create dispatch request DTO
     * @return 201 Created with dispatch ID
     */
    @PostMapping("/dispatches")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<DispatchResponseDto>> createDispatch(
            @Valid @RequestBody CreateDispatchRequestDto requestDto) {

        // Create command from DTO
        CreateDispatchCommand command = new CreateDispatchCommand(requestDto.getDispatchId(), requestDto);

        // Validate command
        var validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<CreateDispatchCommand, DispatchResponseDto> handler =
                commandHandlerRegistry.findHandler(CreateDispatchCommand.class);
        DispatchResponseDto response = handler.handle(command);

        // Return 201 Created response
        return created(response, "Dispatch create request created");
    }
}
