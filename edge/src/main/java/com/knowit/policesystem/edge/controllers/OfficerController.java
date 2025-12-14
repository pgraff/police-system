package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.officers.ChangeOfficerStatusCommand;
import com.knowit.policesystem.edge.commands.officers.BatchRegisterOfficersCommand;
import com.knowit.policesystem.edge.commands.officers.RegisterOfficerCommand;
import com.knowit.policesystem.edge.commands.officers.UpdateOfficerCommand;
import com.knowit.policesystem.edge.dto.ChangeOfficerStatusRequestDto;
import com.knowit.policesystem.edge.dto.BatchCreateOfficersRequestDto;
import com.knowit.policesystem.edge.dto.BatchCreateResponseDto;
import com.knowit.policesystem.edge.dto.OfficerResponseDto;
import com.knowit.policesystem.edge.dto.OfficerStatusResponseDto;
import com.knowit.policesystem.edge.dto.RegisterOfficerRequestDto;
import com.knowit.policesystem.edge.dto.UpdateOfficerRequestDto;
import com.knowit.policesystem.edge.validation.officers.BatchRegisterOfficersCommandValidator;
import com.knowit.policesystem.edge.validation.officers.ChangeOfficerStatusCommandValidator;
import com.knowit.policesystem.edge.validation.officers.RegisterOfficerCommandValidator;
import com.knowit.policesystem.edge.validation.officers.UpdateOfficerCommandValidator;
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
 * REST controller for officer operations.
 * Handles HTTP requests for officer-related endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class OfficerController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final RegisterOfficerCommandValidator registerCommandValidator;
    private final UpdateOfficerCommandValidator updateCommandValidator;
    private final ChangeOfficerStatusCommandValidator changeStatusCommandValidator;
    private final BatchRegisterOfficersCommandValidator batchRegisterCommandValidator;

    /**
     * Creates a new officer controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param registerCommandValidator the register command validator
     * @param updateCommandValidator the update command validator
     * @param changeStatusCommandValidator the change status command validator
     */
    public OfficerController(CommandHandlerRegistry commandHandlerRegistry,
                            RegisterOfficerCommandValidator registerCommandValidator,
                            UpdateOfficerCommandValidator updateCommandValidator,
                            ChangeOfficerStatusCommandValidator changeStatusCommandValidator,
                            BatchRegisterOfficersCommandValidator batchRegisterCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.registerCommandValidator = registerCommandValidator;
        this.updateCommandValidator = updateCommandValidator;
        this.changeStatusCommandValidator = changeStatusCommandValidator;
        this.batchRegisterCommandValidator = batchRegisterCommandValidator;
    }

    /**
     * Registers a new officer.
     * Accepts officer data, validates it, and publishes a RegisterOfficerRequested event to Kafka and NATS/JetStream.
     *
     * @param requestDto the officer registration request DTO
     * @return 201 Created with officer ID and badge number
     */
    @PostMapping("/officers")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<OfficerResponseDto>> registerOfficer(
            @Valid @RequestBody RegisterOfficerRequestDto requestDto) {

        RegisterOfficerCommand command = new RegisterOfficerCommand(requestDto.getBadgeNumber(), requestDto);
        return executeCommand(command, registerCommandValidator, commandHandlerRegistry, RegisterOfficerCommand.class,
                "Officer registration request created", HttpStatus.CREATED);
    }

    /**
     * Batch registers multiple officers.
     * Accepts an array of officer data, validates each, and publishes RegisterOfficerRequested events.
     * Returns a summary of created and failed officers.
     *
     * @param requestDto the batch create officers request DTO
     * @return 201 Created with batch response containing created and failed lists
     */
    @PostMapping("/officers/batch")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<BatchCreateResponseDto>> batchRegisterOfficers(
            @Valid @RequestBody BatchCreateOfficersRequestDto requestDto) {

        String batchId = "BATCH-" + System.currentTimeMillis();
        BatchRegisterOfficersCommand command = new BatchRegisterOfficersCommand(batchId, requestDto);
        return executeCommand(command, batchRegisterCommandValidator, commandHandlerRegistry,
                BatchRegisterOfficersCommand.class, "Batch officer registration completed", HttpStatus.CREATED);
    }

    /**
     * Updates an existing officer.
     * Accepts partial officer data, validates it, and publishes an UpdateOfficerRequested event to Kafka and NATS/JetStream.
     *
     * @param badgeNumber the badge number of the officer to update
     * @param requestDto the officer update request DTO (all fields optional)
     * @return 200 OK with badge number
     */
    @PutMapping("/officers/{badgeNumber}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<OfficerResponseDto>> updateOfficer(
            @PathVariable String badgeNumber,
            @Valid @RequestBody UpdateOfficerRequestDto requestDto) {

        UpdateOfficerCommand command = new UpdateOfficerCommand(badgeNumber, requestDto);
        return executeCommand(command, updateCommandValidator, commandHandlerRegistry, UpdateOfficerCommand.class,
                "Officer update request processed", HttpStatus.OK);
    }

    /**
     * Changes an officer's status.
     * Accepts status change data, validates it, and publishes a ChangeOfficerStatusRequested event to Kafka and NATS/JetStream.
     *
     * @param badgeNumber the badge number of the officer whose status to change
     * @param requestDto the status change request DTO
     * @return 200 OK with badge number and status
     */
    @PatchMapping("/officers/{badgeNumber}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<OfficerStatusResponseDto>> changeOfficerStatus(
            @PathVariable String badgeNumber,
            @Valid @RequestBody ChangeOfficerStatusRequestDto requestDto) {

        ChangeOfficerStatusCommand command = new ChangeOfficerStatusCommand(badgeNumber, requestDto);
        return executeCommand(command, changeStatusCommandValidator, commandHandlerRegistry, ChangeOfficerStatusCommand.class,
                "Officer status change request processed", HttpStatus.OK);
    }
}
