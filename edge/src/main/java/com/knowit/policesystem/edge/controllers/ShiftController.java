package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.shifts.StartShiftCommand;
import com.knowit.policesystem.edge.dto.ShiftResponseDto;
import com.knowit.policesystem.edge.dto.StartShiftRequestDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.shifts.StartShiftCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for shift operations.
 * Handles HTTP requests for starting shifts.
 */
@RestController
@RequestMapping("/api/v1")
public class ShiftController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final StartShiftCommandValidator commandValidator;

    /**
     * Creates a new shift controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the start shift command validator
     */
    public ShiftController(CommandHandlerRegistry commandHandlerRegistry,
                           StartShiftCommandValidator commandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
    }

    /**
     * Starts a new shift.
     * Accepts shift data, validates it, and publishes a StartShiftRequested event to Kafka.
     *
     * @param requestDto the start shift request DTO
     * @return 201 Created with shift ID
     */
    @PostMapping("/shifts")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<ShiftResponseDto>> startShift(
            @Valid @RequestBody StartShiftRequestDto requestDto) {

        StartShiftCommand command = new StartShiftCommand(requestDto.getShiftId(), requestDto);

        var validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        com.knowit.policesystem.edge.commands.CommandHandler<StartShiftCommand, ShiftResponseDto> handler =
                commandHandlerRegistry.findHandler(StartShiftCommand.class);
        ShiftResponseDto response = handler.handle(command);

        return created(response, "Shift start request created");
    }
}
