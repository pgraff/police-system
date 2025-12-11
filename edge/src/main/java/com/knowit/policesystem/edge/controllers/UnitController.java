package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.units.ChangeUnitStatusCommand;
import com.knowit.policesystem.edge.commands.units.CreateUnitCommand;
import com.knowit.policesystem.edge.commands.units.UpdateUnitCommand;
import com.knowit.policesystem.edge.dto.ChangeUnitStatusRequestDto;
import com.knowit.policesystem.edge.dto.CreateUnitRequestDto;
import com.knowit.policesystem.edge.dto.SuccessResponse;
import com.knowit.policesystem.edge.dto.UpdateUnitRequestDto;
import com.knowit.policesystem.edge.dto.UnitResponseDto;
import com.knowit.policesystem.edge.dto.UnitStatusResponseDto;
import com.knowit.policesystem.edge.validation.units.ChangeUnitStatusCommandValidator;
import com.knowit.policesystem.edge.validation.units.CreateUnitCommandValidator;
import com.knowit.policesystem.edge.validation.units.UpdateUnitCommandValidator;
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
 * REST controller for unit operations.
 * Handles HTTP requests for unit-related endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class UnitController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final CreateUnitCommandValidator createCommandValidator;
    private final UpdateUnitCommandValidator updateCommandValidator;
    private final ChangeUnitStatusCommandValidator changeStatusCommandValidator;

    /**
     * Creates a new unit controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param createCommandValidator the create command validator
     * @param updateCommandValidator the update command validator
     * @param changeStatusCommandValidator the change status command validator
     */
    public UnitController(CommandHandlerRegistry commandHandlerRegistry,
                         CreateUnitCommandValidator createCommandValidator,
                         UpdateUnitCommandValidator updateCommandValidator,
                         ChangeUnitStatusCommandValidator changeStatusCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.createCommandValidator = createCommandValidator;
        this.updateCommandValidator = updateCommandValidator;
        this.changeStatusCommandValidator = changeStatusCommandValidator;
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

        CreateUnitCommand command = new CreateUnitCommand(requestDto.getUnitId(), requestDto);
        return executeCommand(command, createCommandValidator, commandHandlerRegistry, CreateUnitCommand.class,
                "Unit creation request processed", HttpStatus.CREATED);
    }

    /**
     * Updates an existing unit.
     * Accepts unit update data, validates it, and publishes an UpdateUnitRequested event to Kafka and NATS/JetStream.
     *
     * @param unitId the unit ID of the unit to update
     * @param requestDto the unit update request DTO
     * @return 200 OK with unit ID
     */
    @PutMapping("/units/{unitId}")
    public ResponseEntity<SuccessResponse<UnitResponseDto>> updateUnit(
            @PathVariable String unitId,
            @Valid @RequestBody UpdateUnitRequestDto requestDto) {

        UpdateUnitCommand command = new UpdateUnitCommand(unitId, requestDto);
        return executeCommand(command, updateCommandValidator, commandHandlerRegistry, UpdateUnitCommand.class,
                "Unit update request processed", HttpStatus.OK);
    }

    /**
     * Changes a unit's status.
     * Accepts status change data, validates it, and publishes a ChangeUnitStatusRequested event to Kafka and NATS/JetStream.
     *
     * @param unitId the unit ID of the unit whose status to change
     * @param requestDto the status change request DTO
     * @return 200 OK with unit ID and status
     */
    @PatchMapping("/units/{unitId}")
    public ResponseEntity<SuccessResponse<UnitStatusResponseDto>> changeUnitStatus(
            @PathVariable String unitId,
            @Valid @RequestBody ChangeUnitStatusRequestDto requestDto) {

        ChangeUnitStatusCommand command = new ChangeUnitStatusCommand(unitId, requestDto);
        return executeCommand(command, changeStatusCommandValidator, commandHandlerRegistry, ChangeUnitStatusCommand.class,
                "Unit status change request processed", HttpStatus.OK);
    }
}
