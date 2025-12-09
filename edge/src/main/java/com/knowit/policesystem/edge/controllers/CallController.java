package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.calls.ReceiveCallCommand;
import com.knowit.policesystem.edge.dto.CallResponseDto;
import com.knowit.policesystem.edge.dto.ReceiveCallRequestDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.calls.ReceiveCallCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for call operations.
 * Handles HTTP requests for call-related endpoints.
 */
@RestController
public class CallController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final ReceiveCallCommandValidator commandValidator;

    /**
     * Creates a new call controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the receive call command validator
     */
    public CallController(CommandHandlerRegistry commandHandlerRegistry,
                          ReceiveCallCommandValidator commandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
    }

    /**
     * Receives a new call.
     * Accepts call data, validates it, and publishes a ReceiveCallRequested event to Kafka.
     *
     * @param requestDto the receive call request DTO
     * @return 201 Created with call ID and number
     */
    @PostMapping("/calls")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<CallResponseDto>> receiveCall(
            @Valid @RequestBody ReceiveCallRequestDto requestDto) {

        // Create command from DTO
        ReceiveCallCommand command = new ReceiveCallCommand(requestDto.getCallId(), requestDto);

        // Validate command
        var validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<ReceiveCallCommand, CallResponseDto> handler =
                commandHandlerRegistry.findHandler(ReceiveCallCommand.class);
        CallResponseDto response = handler.handle(command);

        // Return 201 Created response
        return created(response, "Call receive request created");
    }
}
