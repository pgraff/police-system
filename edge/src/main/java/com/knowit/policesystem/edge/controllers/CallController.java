package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.calls.ArriveAtCallCommand;
import com.knowit.policesystem.edge.commands.calls.ClearCallCommand;
import com.knowit.policesystem.edge.commands.calls.DispatchCallCommand;
import com.knowit.policesystem.edge.commands.calls.ReceiveCallCommand;
import com.knowit.policesystem.edge.dto.CallResponseDto;
import com.knowit.policesystem.edge.dto.ArriveAtCallRequestDto;
import com.knowit.policesystem.edge.dto.ClearCallRequestDto;
import com.knowit.policesystem.edge.dto.DispatchCallRequestDto;
import com.knowit.policesystem.edge.dto.ReceiveCallRequestDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.calls.ArriveAtCallCommandValidator;
import com.knowit.policesystem.edge.validation.calls.ClearCallCommandValidator;
import com.knowit.policesystem.edge.validation.calls.DispatchCallCommandValidator;
import com.knowit.policesystem.edge.validation.calls.ReceiveCallCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for call operations.
 * Handles HTTP requests for call-related endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class CallController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final ReceiveCallCommandValidator commandValidator;
    private final DispatchCallCommandValidator dispatchCommandValidator;
    private final ArriveAtCallCommandValidator arriveAtCallCommandValidator;
    private final ClearCallCommandValidator clearCallCommandValidator;

    /**
     * Creates a new call controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the receive call command validator
     */
    public CallController(CommandHandlerRegistry commandHandlerRegistry,
                          ReceiveCallCommandValidator commandValidator,
                          DispatchCallCommandValidator dispatchCommandValidator,
                          ArriveAtCallCommandValidator arriveAtCallCommandValidator,
                          ClearCallCommandValidator clearCallCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
        this.dispatchCommandValidator = dispatchCommandValidator;
        this.arriveAtCallCommandValidator = arriveAtCallCommandValidator;
        this.clearCallCommandValidator = clearCallCommandValidator;
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

    /**
     * Dispatches a call.
     * Validates the dispatch request and publishes a DispatchCallRequested event to Kafka.
     *
     * @param callId the call identifier
     * @param requestDto the dispatch request DTO
     * @return 200 OK with callId
     */
    @PostMapping("/calls/{callId}/dispatch")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<CallResponseDto>> dispatchCall(
            @PathVariable String callId,
            @Valid @RequestBody DispatchCallRequestDto requestDto) {

        DispatchCallCommand command = new DispatchCallCommand(callId, requestDto);

        var validationResult = dispatchCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        com.knowit.policesystem.edge.commands.CommandHandler<DispatchCallCommand, CallResponseDto> handler =
                commandHandlerRegistry.findHandler(DispatchCallCommand.class);
        CallResponseDto response = handler.handle(command);

        return success(response, "Call dispatch recorded");
    }

    /**
     * Records arrival at a call.
     *
     * @param callId the call identifier
     * @param requestDto the arrival request DTO
     * @return 200 OK with callId
     */
    @PostMapping("/calls/{callId}/arrive")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<CallResponseDto>> arriveAtCall(
            @PathVariable String callId,
            @Valid @RequestBody ArriveAtCallRequestDto requestDto) {

        ArriveAtCallCommand command = new ArriveAtCallCommand(callId, requestDto);

        var validationResult = arriveAtCallCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        com.knowit.policesystem.edge.commands.CommandHandler<ArriveAtCallCommand, CallResponseDto> handler =
                commandHandlerRegistry.findHandler(ArriveAtCallCommand.class);
        CallResponseDto response = handler.handle(command);

        return success(response, "Call arrival recorded");
    }

    /**
    * Clears a call.
    *
    * @param callId the call identifier
    * @param requestDto the clear request DTO
    * @return 200 OK with callId
    */
    @PostMapping("/calls/{callId}/clear")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<CallResponseDto>> clearCall(
            @PathVariable String callId,
            @Valid @RequestBody ClearCallRequestDto requestDto) {

        ClearCallCommand command = new ClearCallCommand(callId, requestDto);

        var validationResult = clearCallCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        com.knowit.policesystem.edge.commands.CommandHandler<ClearCallCommand, CallResponseDto> handler =
                commandHandlerRegistry.findHandler(ClearCallCommand.class);
        CallResponseDto response = handler.handle(command);

        return success(response, "Call cleared");
    }
}
