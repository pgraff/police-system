package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.dispatches.ChangeDispatchStatusCommand;
import com.knowit.policesystem.edge.commands.dispatches.CreateDispatchCommand;
import com.knowit.policesystem.edge.dto.ChangeDispatchStatusRequestDto;
import com.knowit.policesystem.edge.dto.CreateDispatchRequestDto;
import com.knowit.policesystem.edge.dto.DispatchResponseDto;
import com.knowit.policesystem.edge.validation.dispatches.ChangeDispatchStatusCommandValidator;
import com.knowit.policesystem.edge.validation.dispatches.CreateDispatchCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final ChangeDispatchStatusCommandValidator changeDispatchStatusCommandValidator;

    /**
     * Creates a new dispatch controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the create dispatch command validator
     * @param changeDispatchStatusCommandValidator the change dispatch status command validator
     */
    public DispatchController(CommandHandlerRegistry commandHandlerRegistry,
                              CreateDispatchCommandValidator commandValidator,
                              ChangeDispatchStatusCommandValidator changeDispatchStatusCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
        this.changeDispatchStatusCommandValidator = changeDispatchStatusCommandValidator;
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

        CreateDispatchCommand command = new CreateDispatchCommand(requestDto.getDispatchId(), requestDto);
        return executeCommand(command, commandValidator, commandHandlerRegistry, CreateDispatchCommand.class,
                "Dispatch create request created", HttpStatus.CREATED);
    }

    /**
     * Changes a dispatch's status.
     * Accepts status change data, validates it, and publishes a ChangeDispatchStatusRequested event to Kafka.
     *
     * @param dispatchId the dispatch identifier from the path
     * @param requestDto the change dispatch status request DTO
     * @return 200 OK with dispatch ID
     */
    @PatchMapping("/dispatches/{dispatchId}/status")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<DispatchResponseDto>> changeDispatchStatus(
            @PathVariable String dispatchId,
            @Valid @RequestBody ChangeDispatchStatusRequestDto requestDto) {

        ChangeDispatchStatusCommand command = new ChangeDispatchStatusCommand(dispatchId, requestDto);
        return executeCommand(command, changeDispatchStatusCommandValidator, commandHandlerRegistry, ChangeDispatchStatusCommand.class,
                "Dispatch status change request processed", HttpStatus.OK);
    }
}
