package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.vehicles.RegisterVehicleCommand;
import com.knowit.policesystem.edge.commands.vehicles.UpdateVehicleCommand;
import com.knowit.policesystem.edge.dto.RegisterVehicleRequestDto;
import com.knowit.policesystem.edge.dto.UpdateVehicleRequestDto;
import com.knowit.policesystem.edge.dto.VehicleResponseDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.vehicles.RegisterVehicleCommandValidator;
import com.knowit.policesystem.edge.validation.vehicles.UpdateVehicleCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final UpdateVehicleCommandValidator updateCommandValidator;

    /**
     * Creates a new vehicle controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param registerCommandValidator the register command validator
     * @param updateCommandValidator the update command validator
     */
    public VehicleController(CommandHandlerRegistry commandHandlerRegistry,
                              RegisterVehicleCommandValidator registerCommandValidator,
                              UpdateVehicleCommandValidator updateCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.registerCommandValidator = registerCommandValidator;
        this.updateCommandValidator = updateCommandValidator;
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

    /**
     * Updates an existing vehicle.
     * Accepts partial vehicle data, validates it, and publishes an UpdateVehicleRequested event to Kafka and NATS/JetStream.
     *
     * @param unitId the unit ID of the vehicle to update
     * @param requestDto the vehicle update request DTO (all fields optional)
     * @return 200 OK with vehicle ID and unit ID
     */
    @PutMapping("/vehicles/{unitId}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<VehicleResponseDto>> updateVehicle(
            @PathVariable String unitId,
            @Valid @RequestBody UpdateVehicleRequestDto requestDto) {

        // Create command from DTO
        UpdateVehicleCommand command = new UpdateVehicleCommand(unitId, requestDto);

        // Validate command
        var validationResult = updateCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<UpdateVehicleCommand, VehicleResponseDto> handler =
                commandHandlerRegistry.findHandler(UpdateVehicleCommand.class);
        VehicleResponseDto response = handler.handle(command);

        // Return 200 OK response
        return success(response, "Vehicle update request processed");
    }
}
