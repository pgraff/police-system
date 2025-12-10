package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.shifts.ChangeShiftStatusCommand;
import com.knowit.policesystem.edge.commands.shifts.CheckInOfficerCommand;
import com.knowit.policesystem.edge.commands.shifts.EndShiftCommand;
import com.knowit.policesystem.edge.commands.shifts.RecordShiftChangeCommand;
import com.knowit.policesystem.edge.commands.shifts.StartShiftCommand;
import com.knowit.policesystem.edge.dto.ChangeShiftStatusRequestDto;
import com.knowit.policesystem.edge.dto.CheckInOfficerRequestDto;
import com.knowit.policesystem.edge.dto.RecordShiftChangeRequestDto;
import com.knowit.policesystem.edge.dto.ShiftChangeResponseDto;
import com.knowit.policesystem.edge.dto.ShiftResponseDto;
import com.knowit.policesystem.edge.dto.EndShiftRequestDto;
import com.knowit.policesystem.edge.dto.OfficerShiftResponseDto;
import com.knowit.policesystem.edge.dto.StartShiftRequestDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.shifts.ChangeShiftStatusCommandValidator;
import com.knowit.policesystem.edge.validation.shifts.CheckInOfficerCommandValidator;
import com.knowit.policesystem.edge.validation.shifts.EndShiftCommandValidator;
import com.knowit.policesystem.edge.validation.shifts.RecordShiftChangeCommandValidator;
import com.knowit.policesystem.edge.validation.shifts.StartShiftCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final EndShiftCommandValidator endShiftCommandValidator;
    private final ChangeShiftStatusCommandValidator changeShiftStatusCommandValidator;
    private final RecordShiftChangeCommandValidator recordShiftChangeCommandValidator;
    private final CheckInOfficerCommandValidator checkInOfficerCommandValidator;

    /**
     * Creates a new shift controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the start shift command validator
     * @param endShiftCommandValidator the end shift command validator
     * @param changeShiftStatusCommandValidator the change shift status command validator
     * @param recordShiftChangeCommandValidator the record shift change command validator
     * @param checkInOfficerCommandValidator the check-in officer command validator
     */
    public ShiftController(CommandHandlerRegistry commandHandlerRegistry,
                           StartShiftCommandValidator commandValidator,
                           EndShiftCommandValidator endShiftCommandValidator,
                           ChangeShiftStatusCommandValidator changeShiftStatusCommandValidator,
                           RecordShiftChangeCommandValidator recordShiftChangeCommandValidator,
                           CheckInOfficerCommandValidator checkInOfficerCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
        this.endShiftCommandValidator = endShiftCommandValidator;
        this.changeShiftStatusCommandValidator = changeShiftStatusCommandValidator;
        this.recordShiftChangeCommandValidator = recordShiftChangeCommandValidator;
        this.checkInOfficerCommandValidator = checkInOfficerCommandValidator;
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

    /**
     * Ends a shift.
     * Accepts end time, validates it, and publishes an EndShiftRequested event to Kafka.
     *
     * @param shiftId the shift identifier from the path
     * @param requestDto the end shift request DTO
     * @return 200 OK with shift ID
     */
    @PostMapping("/shifts/{shiftId}/end")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<ShiftResponseDto>> endShift(
            @PathVariable String shiftId,
            @Valid @RequestBody EndShiftRequestDto requestDto) {

        EndShiftCommand command = new EndShiftCommand(shiftId, requestDto);

        var validationResult = endShiftCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        com.knowit.policesystem.edge.commands.CommandHandler<EndShiftCommand, ShiftResponseDto> handler =
                commandHandlerRegistry.findHandler(EndShiftCommand.class);
        ShiftResponseDto response = handler.handle(command);

        return success(response, "Shift end request processed");
    }

    /**
     * Changes a shift's status.
     * Accepts status, validates it, and publishes a ChangeShiftStatusRequested event to Kafka.
     *
     * @param shiftId the shift identifier from the path
     * @param requestDto the change shift status request DTO
     * @return 200 OK with shift ID
     */
    @PatchMapping("/shifts/{shiftId}/status")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<ShiftResponseDto>> changeShiftStatus(
            @PathVariable String shiftId,
            @Valid @RequestBody ChangeShiftStatusRequestDto requestDto) {

        ChangeShiftStatusCommand command = new ChangeShiftStatusCommand(shiftId, requestDto);

        var validationResult = changeShiftStatusCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        com.knowit.policesystem.edge.commands.CommandHandler<ChangeShiftStatusCommand, ShiftResponseDto> handler =
                commandHandlerRegistry.findHandler(ChangeShiftStatusCommand.class);
        ShiftResponseDto response = handler.handle(command);

        return success(response, "Shift status change request processed");
    }

    /**
     * Records a shift change.
     * Accepts shift change data, validates it, and publishes a RecordShiftChangeRequested event to Kafka.
     *
     * @param shiftId the shift identifier from the path
     * @param requestDto the record shift change request DTO
     * @return 201 Created with shift change ID
     */
    @PostMapping("/shifts/{shiftId}/shift-changes")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<ShiftChangeResponseDto>> recordShiftChange(
            @PathVariable String shiftId,
            @Valid @RequestBody RecordShiftChangeRequestDto requestDto) {

        RecordShiftChangeCommand command = new RecordShiftChangeCommand(shiftId, requestDto);

        var validationResult = recordShiftChangeCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        com.knowit.policesystem.edge.commands.CommandHandler<RecordShiftChangeCommand, ShiftChangeResponseDto> handler =
                commandHandlerRegistry.findHandler(RecordShiftChangeCommand.class);
        ShiftChangeResponseDto response = handler.handle(command);

        return created(response, "Shift change record request processed");
    }

    /**
     * Checks in an officer to a shift.
     * Accepts check-in time and shift role type, validates it, and publishes a CheckInOfficerRequested event to Kafka.
     *
     * @param shiftId the shift identifier from the path
     * @param badgeNumber the badge number from the path
     * @param requestDto the check-in officer request DTO
     * @return 200 OK with shift ID and badge number
     */
    @PostMapping("/shifts/{shiftId}/officers/{badgeNumber}/check-in")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<OfficerShiftResponseDto>> checkInOfficer(
            @PathVariable String shiftId,
            @PathVariable String badgeNumber,
            @Valid @RequestBody CheckInOfficerRequestDto requestDto) {

        CheckInOfficerCommand command = new CheckInOfficerCommand(shiftId, badgeNumber, requestDto);

        var validationResult = checkInOfficerCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        com.knowit.policesystem.edge.commands.CommandHandler<CheckInOfficerCommand, OfficerShiftResponseDto> handler =
                commandHandlerRegistry.findHandler(CheckInOfficerCommand.class);
        OfficerShiftResponseDto response = handler.handle(command);

        return success(response, "Officer check-in request processed");
    }
}
