package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.vehicles.RegisterVehicleCommand;
import com.knowit.policesystem.edge.dto.RegisterVehicleRequestDto;
import com.knowit.policesystem.edge.dto.VehicleResponseDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.vehicles.RegisterVehicleCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for vehicle operations.
 * Handles HTTP requests for vehicle-related endpoints.
 */
@RestController
public class VehicleController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final RegisterVehicleCommandValidator registerCommandValidator;

    /**
     * Creates a new vehicle controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param registerCommandValidator the register command validator
     */
    public VehicleController(CommandHandlerRegistry commandHandlerRegistry,
                              RegisterVehicleCommandValidator registerCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.registerCommandValidator = registerCommandValidator;
    }

    /**
     * Registers a new vehicle.
     * Accepts vehicle data, validates it, and publishes a RegisterVehicleRequested event to Kafka and NATS/JetStream.
     *
     * @param requestDto the vehicle registration request DTO
     * @return 201 Created with vehicle ID and unit ID
     */
    @PostMapping("/vehicles")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<VehicleResponseDto>> registerVehicle(
            @Valid @RequestBody RegisterVehicleRequestDto requestDto) {

        // Create command from DTO
        RegisterVehicleCommand command = new RegisterVehicleCommand(requestDto.getUnitId(), requestDto);

        // Validate command
        var validationResult = registerCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<RegisterVehicleCommand, VehicleResponseDto> handler =
                commandHandlerRegistry.findHandler(RegisterVehicleCommand.class);
        VehicleResponseDto response = handler.handle(command);

        // Return 201 Created response
        return created(response, "Vehicle registration request created");
    }
}
