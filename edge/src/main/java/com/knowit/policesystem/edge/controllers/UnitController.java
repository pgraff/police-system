package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.units.CreateUnitCommand;
import com.knowit.policesystem.edge.dto.CreateUnitRequestDto;
import com.knowit.policesystem.edge.dto.SuccessResponse;
import com.knowit.policesystem.edge.dto.UnitResponseDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.units.CreateUnitCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for unit operations.
 * Handles HTTP requests for unit-related endpoints.
 */
@RestController
public class UnitController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final CreateUnitCommandValidator createCommandValidator;

    /**
     * Creates a new unit controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param createCommandValidator the create command validator
     */
    public UnitController(CommandHandlerRegistry commandHandlerRegistry,
                         CreateUnitCommandValidator createCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.createCommandValidator = createCommandValidator;
    }

    /**
     * Creates a new unit.
     * Accepts unit data, validates it, and publishes a CreateUnitRequested event to Kafka and NATS/JetStream.
     *
     * @param requestDto the unit creation request DTO
     * @return 201 Created with unit ID
     */
    @PostMapping("/units")
    public ResponseEntity<SuccessResponse<UnitResponseDto>> createUnit(
            @Valid @RequestBody CreateUnitRequestDto requestDto) {

        // Create command from DTO
        CreateUnitCommand command = new CreateUnitCommand(requestDto.getUnitId(), requestDto);

        // Validate command
        var validationResult = createCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<CreateUnitCommand, UnitResponseDto> handler =
                commandHandlerRegistry.findHandler(CreateUnitCommand.class);
        UnitResponseDto response = handler.handle(command);

        // Return 201 Created response
        return created(response, "Unit creation request processed");
    }
}
