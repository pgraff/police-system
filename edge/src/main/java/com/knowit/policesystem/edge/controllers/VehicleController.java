package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.vehicles.ChangeVehicleStatusCommand;
import com.knowit.policesystem.edge.commands.vehicles.RegisterVehicleCommand;
import com.knowit.policesystem.edge.commands.vehicles.UpdateVehicleCommand;
import com.knowit.policesystem.edge.dto.ChangeVehicleStatusRequestDto;
import com.knowit.policesystem.edge.dto.RegisterVehicleRequestDto;
import com.knowit.policesystem.edge.dto.UpdateVehicleRequestDto;
import com.knowit.policesystem.edge.dto.VehicleResponseDto;
import com.knowit.policesystem.edge.dto.VehicleStatusResponseDto;
import com.knowit.policesystem.edge.validation.vehicles.ChangeVehicleStatusCommandValidator;
import com.knowit.policesystem.edge.validation.vehicles.RegisterVehicleCommandValidator;
import com.knowit.policesystem.edge.validation.vehicles.UpdateVehicleCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for vehicle operations.
 * Handles HTTP requests for vehicle-related endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class VehicleController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final RegisterVehicleCommandValidator registerCommandValidator;
    private final UpdateVehicleCommandValidator updateCommandValidator;
    private final ChangeVehicleStatusCommandValidator changeStatusCommandValidator;

    /**
     * Creates a new vehicle controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param registerCommandValidator the register command validator
     * @param updateCommandValidator the update command validator
     * @param changeStatusCommandValidator the change status command validator
     */
    public VehicleController(CommandHandlerRegistry commandHandlerRegistry,
                              RegisterVehicleCommandValidator registerCommandValidator,
                              UpdateVehicleCommandValidator updateCommandValidator,
                              ChangeVehicleStatusCommandValidator changeStatusCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.registerCommandValidator = registerCommandValidator;
        this.updateCommandValidator = updateCommandValidator;
        this.changeStatusCommandValidator = changeStatusCommandValidator;
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

        RegisterVehicleCommand command = new RegisterVehicleCommand(requestDto.getUnitId(), requestDto);
        return executeCommand(command, registerCommandValidator, commandHandlerRegistry, RegisterVehicleCommand.class,
                "Vehicle registration request created", HttpStatus.CREATED);
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

        UpdateVehicleCommand command = new UpdateVehicleCommand(unitId, requestDto);
        return executeCommand(command, updateCommandValidator, commandHandlerRegistry, UpdateVehicleCommand.class,
                "Vehicle update request processed", HttpStatus.OK);
    }

    /**
     * Changes a vehicle's status.
     * Accepts status change data, validates it, and publishes a ChangeVehicleStatusRequested event to Kafka and NATS/JetStream.
     *
     * @param unitId the unit ID of the vehicle whose status to change
     * @param requestDto the status change request DTO
     * @return 200 OK with unit ID and status
     */
    @PatchMapping("/vehicles/{unitId}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<VehicleStatusResponseDto>> changeVehicleStatus(
            @PathVariable String unitId,
            @Valid @RequestBody ChangeVehicleStatusRequestDto requestDto) {

        ChangeVehicleStatusCommand command = new ChangeVehicleStatusCommand(unitId, requestDto);
        return executeCommand(command, changeStatusCommandValidator, commandHandlerRegistry, ChangeVehicleStatusCommand.class,
                "Vehicle status change request processed", HttpStatus.OK);
    }
}
