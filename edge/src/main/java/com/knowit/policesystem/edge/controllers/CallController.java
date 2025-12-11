package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.calls.ArriveAtCallCommand;
import com.knowit.policesystem.edge.commands.calls.ChangeCallStatusCommand;
import com.knowit.policesystem.edge.commands.calls.ClearCallCommand;
import com.knowit.policesystem.edge.commands.calls.DispatchCallCommand;
import com.knowit.policesystem.edge.commands.calls.LinkCallToIncidentCommand;
import com.knowit.policesystem.edge.commands.calls.LinkCallToDispatchCommand;
import com.knowit.policesystem.edge.commands.calls.ReceiveCallCommand;
import com.knowit.policesystem.edge.commands.calls.UpdateCallCommand;
import com.knowit.policesystem.edge.dto.CallResponseDto;
import com.knowit.policesystem.edge.dto.ArriveAtCallRequestDto;
import com.knowit.policesystem.edge.dto.ChangeCallStatusRequestDto;
import com.knowit.policesystem.edge.dto.ClearCallRequestDto;
import com.knowit.policesystem.edge.dto.DispatchCallRequestDto;
import com.knowit.policesystem.edge.dto.LinkCallToIncidentRequestDto;
import com.knowit.policesystem.edge.dto.LinkCallToIncidentResponseDto;
import com.knowit.policesystem.edge.dto.LinkCallToDispatchRequestDto;
import com.knowit.policesystem.edge.dto.LinkCallToDispatchResponseDto;
import com.knowit.policesystem.edge.dto.ReceiveCallRequestDto;
import com.knowit.policesystem.edge.dto.UpdateCallRequestDto;
import com.knowit.policesystem.edge.validation.calls.ArriveAtCallCommandValidator;
import com.knowit.policesystem.edge.validation.calls.ChangeCallStatusCommandValidator;
import com.knowit.policesystem.edge.validation.calls.ClearCallCommandValidator;
import com.knowit.policesystem.edge.validation.calls.DispatchCallCommandValidator;
import com.knowit.policesystem.edge.validation.calls.LinkCallToIncidentCommandValidator;
import com.knowit.policesystem.edge.validation.calls.LinkCallToDispatchCommandValidator;
import com.knowit.policesystem.edge.validation.calls.ReceiveCallCommandValidator;
import com.knowit.policesystem.edge.validation.calls.UpdateCallCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;

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
    private final ChangeCallStatusCommandValidator changeCallStatusCommandValidator;
    private final UpdateCallCommandValidator updateCallCommandValidator;
    private final LinkCallToIncidentCommandValidator linkCallToIncidentCommandValidator;
    private final LinkCallToDispatchCommandValidator linkCallToDispatchCommandValidator;

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
                              ClearCallCommandValidator clearCallCommandValidator,
                              ChangeCallStatusCommandValidator changeCallStatusCommandValidator,
                              UpdateCallCommandValidator updateCallCommandValidator,
                              LinkCallToIncidentCommandValidator linkCallToIncidentCommandValidator,
                              LinkCallToDispatchCommandValidator linkCallToDispatchCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
        this.dispatchCommandValidator = dispatchCommandValidator;
        this.arriveAtCallCommandValidator = arriveAtCallCommandValidator;
        this.clearCallCommandValidator = clearCallCommandValidator;
        this.changeCallStatusCommandValidator = changeCallStatusCommandValidator;
        this.updateCallCommandValidator = updateCallCommandValidator;
        this.linkCallToIncidentCommandValidator = linkCallToIncidentCommandValidator;
        this.linkCallToDispatchCommandValidator = linkCallToDispatchCommandValidator;
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

        ReceiveCallCommand command = new ReceiveCallCommand(requestDto.getCallId(), requestDto);
        return executeCommand(command, commandValidator, commandHandlerRegistry, ReceiveCallCommand.class,
                "Call receive request created", HttpStatus.CREATED);
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
        return executeCommand(command, dispatchCommandValidator, commandHandlerRegistry, DispatchCallCommand.class,
                "Call dispatch recorded", HttpStatus.OK);
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
        return executeCommand(command, arriveAtCallCommandValidator, commandHandlerRegistry, ArriveAtCallCommand.class,
                "Call arrival recorded", HttpStatus.OK);
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
        return executeCommand(command, clearCallCommandValidator, commandHandlerRegistry, ClearCallCommand.class,
                "Call cleared", HttpStatus.OK);
    }

    /**
     * Changes the status of a call.
     *
     * @param callId the call identifier
     * @param requestDto the change status request DTO
     * @return 200 OK with callId
     */
    @PatchMapping("/calls/{callId}/status")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<CallResponseDto>> changeCallStatus(
            @PathVariable String callId,
            @Valid @RequestBody ChangeCallStatusRequestDto requestDto) {

        ChangeCallStatusCommand command = new ChangeCallStatusCommand(callId, requestDto);
        return executeCommand(command, changeCallStatusCommandValidator, commandHandlerRegistry, ChangeCallStatusCommand.class,
                "Call status updated", HttpStatus.OK);
    }

    /**
     * Updates call details.
     *
     * @param callId the call identifier
     * @param requestDto the update request DTO
     * @return 200 OK with callId
     */
    @PutMapping("/calls/{callId}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<CallResponseDto>> updateCall(
            @PathVariable String callId,
            @Valid @RequestBody UpdateCallRequestDto requestDto) {

        UpdateCallCommand command = new UpdateCallCommand(callId, requestDto);
        return executeCommand(command, updateCallCommandValidator, commandHandlerRegistry, UpdateCallCommand.class,
                "Call updated", HttpStatus.OK);
    }

    /**
     * Links a call to an incident.
     * Accepts incident link data, validates it, and publishes a LinkCallToIncidentRequested event to Kafka.
     *
     * @param callId the call ID from the path
     * @param requestDto the incident link request DTO
     * @return 200 OK with callId and incidentId
     */
    @PostMapping("/calls/{callId}/incidents")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<LinkCallToIncidentResponseDto>> linkCallToIncident(
            @PathVariable String callId,
            @Valid @RequestBody LinkCallToIncidentRequestDto requestDto) {

        LinkCallToIncidentCommand command = new LinkCallToIncidentCommand(callId, callId, requestDto);
        return executeCommand(command, linkCallToIncidentCommandValidator, commandHandlerRegistry, LinkCallToIncidentCommand.class,
                "Call link request processed", HttpStatus.OK);
    }

    /**
     * Links a call to a dispatch.
     * Accepts dispatch link data, validates it, and publishes a LinkCallToDispatchRequested event to Kafka.
     *
     * @param callId the call ID from the path
     * @param requestDto the dispatch link request DTO
     * @return 200 OK with callId and dispatchId
     */
    @PostMapping("/calls/{callId}/dispatches")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<LinkCallToDispatchResponseDto>> linkCallToDispatch(
            @PathVariable String callId,
            @Valid @RequestBody LinkCallToDispatchRequestDto requestDto) {

        LinkCallToDispatchCommand command = new LinkCallToDispatchCommand(callId, callId, requestDto);
        return executeCommand(command, linkCallToDispatchCommandValidator, commandHandlerRegistry, LinkCallToDispatchCommand.class,
                "Call link request processed", HttpStatus.OK);
    }
}
