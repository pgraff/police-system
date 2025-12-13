package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.incidents.ArriveAtIncidentCommand;
import com.knowit.policesystem.edge.commands.incidents.ChangeIncidentStatusCommand;
import com.knowit.policesystem.edge.commands.incidents.ClearIncidentCommand;
import com.knowit.policesystem.edge.commands.incidents.DispatchIncidentCommand;
import com.knowit.policesystem.edge.commands.incidents.ReportIncidentCommand;
import com.knowit.policesystem.edge.commands.incidents.UpdateIncidentCommand;
import com.knowit.policesystem.edge.dto.ArriveAtIncidentRequestDto;
import com.knowit.policesystem.edge.dto.ChangeIncidentStatusRequestDto;
import com.knowit.policesystem.edge.dto.ClearIncidentRequestDto;
import com.knowit.policesystem.edge.dto.DispatchIncidentRequestDto;
import com.knowit.policesystem.edge.dto.IncidentResponseDto;
import com.knowit.policesystem.edge.dto.ReportIncidentRequestDto;
import com.knowit.policesystem.edge.dto.UpdateIncidentRequestDto;
import com.knowit.policesystem.edge.validation.incidents.ArriveAtIncidentCommandValidator;
import com.knowit.policesystem.edge.validation.incidents.ChangeIncidentStatusCommandValidator;
import com.knowit.policesystem.edge.validation.incidents.ClearIncidentCommandValidator;
import com.knowit.policesystem.edge.validation.incidents.DispatchIncidentCommandValidator;
import com.knowit.policesystem.edge.validation.incidents.ReportIncidentCommandValidator;
import com.knowit.policesystem.edge.validation.incidents.UpdateIncidentCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for incident operations.
 * Handles HTTP requests for incident-related endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class IncidentController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final ReportIncidentCommandValidator commandValidator;
    private final DispatchIncidentCommandValidator dispatchCommandValidator;
    private final ArriveAtIncidentCommandValidator arriveCommandValidator;
    private final ClearIncidentCommandValidator clearCommandValidator;
    private final ChangeIncidentStatusCommandValidator changeStatusCommandValidator;
    private final UpdateIncidentCommandValidator updateIncidentCommandValidator;

    /**
     * Creates a new incident controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the report incident command validator
     * @param dispatchCommandValidator the dispatch incident command validator
     * @param arriveCommandValidator the arrive incident command validator
     * @param clearCommandValidator the clear incident command validator
     * @param changeStatusCommandValidator the change status command validator
     * @param updateIncidentCommandValidator the update incident command validator
     */
    public IncidentController(CommandHandlerRegistry commandHandlerRegistry,
                              ReportIncidentCommandValidator commandValidator,
                              DispatchIncidentCommandValidator dispatchCommandValidator,
                              ArriveAtIncidentCommandValidator arriveCommandValidator,
                              ClearIncidentCommandValidator clearCommandValidator,
                              ChangeIncidentStatusCommandValidator changeStatusCommandValidator,
                              UpdateIncidentCommandValidator updateIncidentCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
        this.dispatchCommandValidator = dispatchCommandValidator;
        this.arriveCommandValidator = arriveCommandValidator;
        this.clearCommandValidator = clearCommandValidator;
        this.changeStatusCommandValidator = changeStatusCommandValidator;
        this.updateIncidentCommandValidator = updateIncidentCommandValidator;
    }

    /**
     * Reports a new incident.
     * Accepts incident data, validates it, and publishes a ReportIncidentRequested event to Kafka.
     *
     * @param requestDto the incident report request DTO
     * @return 201 Created with incident ID and number
     */
    @PostMapping("/incidents")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<IncidentResponseDto>> reportIncident(
            @Valid @RequestBody ReportIncidentRequestDto requestDto) {

        ReportIncidentCommand command = new ReportIncidentCommand(requestDto.getIncidentId(), requestDto);
        return executeCommand(command, commandValidator, commandHandlerRegistry, ReportIncidentCommand.class,
                "Incident report request created", HttpStatus.CREATED);
    }

    /**
     * Dispatches an incident.
     * Accepts dispatch time, validates it, and publishes a DispatchIncidentRequested event.
     *
     * @param incidentId the incident identifier from the path
     * @param requestDto the dispatch request DTO
     * @return 200 OK with incident ID
     */
    @PostMapping("/incidents/{incidentId}/dispatch")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<IncidentResponseDto>> dispatchIncident(
            @PathVariable String incidentId,
            @Valid @RequestBody DispatchIncidentRequestDto requestDto) {

        DispatchIncidentCommand command = new DispatchIncidentCommand(incidentId, requestDto);
        return executeCommand(command, dispatchCommandValidator, commandHandlerRegistry, DispatchIncidentCommand.class,
                "Incident dispatch request created", HttpStatus.OK);
    }

    /**
     * Marks an incident as arrived.
     * Accepts arrival time, validates it, and publishes an ArriveAtIncidentRequested event.
     *
     * @param incidentId the incident identifier from the path
     * @param requestDto the arrival request DTO
     * @return 200 OK with incident ID
     */
    @PostMapping("/incidents/{incidentId}/arrive")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<IncidentResponseDto>> arriveAtIncident(
            @PathVariable String incidentId,
            @Valid @RequestBody ArriveAtIncidentRequestDto requestDto) {

        ArriveAtIncidentCommand command = new ArriveAtIncidentCommand(incidentId, requestDto);
        return executeCommand(command, arriveCommandValidator, commandHandlerRegistry, ArriveAtIncidentCommand.class,
                "Incident arrival request created", HttpStatus.OK);
    }

    /**
     * Clears an incident.
     * Accepts cleared time, validates it, and publishes a ClearIncidentRequested event.
     *
     * @param incidentId the incident identifier from the path
     * @param requestDto the clear request DTO
     * @return 200 OK with incident ID
     */
    @PostMapping("/incidents/{incidentId}/clear")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<IncidentResponseDto>> clearIncident(
            @PathVariable String incidentId,
            @Valid @RequestBody ClearIncidentRequestDto requestDto) {

        ClearIncidentCommand command = new ClearIncidentCommand(incidentId, requestDto);
        return executeCommand(command, clearCommandValidator, commandHandlerRegistry, ClearIncidentCommand.class,
                "Incident clear request created", HttpStatus.OK);
    }

    /**
     * Changes an incident's status.
     * Accepts a new status, validates it, and publishes a ChangeIncidentStatusRequested event.
     *
     * @param incidentId the incident identifier from the path
     * @param requestDto the change status request DTO
     * @return 200 OK with incident ID
     */
    @PatchMapping("/incidents/{incidentId}/status")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<IncidentResponseDto>> changeIncidentStatus(
            @PathVariable String incidentId,
            @Valid @RequestBody ChangeIncidentStatusRequestDto requestDto) {

        ChangeIncidentStatusCommand command = new ChangeIncidentStatusCommand(incidentId, requestDto);
        return executeCommand(command, changeStatusCommandValidator, commandHandlerRegistry, ChangeIncidentStatusCommand.class,
                "Incident status change request created", HttpStatus.OK);
    }

    /**
     * Updates an incident.
     * Accepts incident update data, validates it, and publishes an UpdateIncidentRequested event.
     *
     * @param incidentId the incident identifier from the path
     * @param requestDto the update incident request DTO
     * @return 200 OK with incident ID
     */
    @PutMapping("/incidents/{incidentId}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<IncidentResponseDto>> updateIncident(
            @PathVariable String incidentId,
            @Valid @RequestBody UpdateIncidentRequestDto requestDto) {

        UpdateIncidentCommand command = new UpdateIncidentCommand(incidentId, requestDto);
        return executeCommand(command, updateIncidentCommandValidator, commandHandlerRegistry, UpdateIncidentCommand.class,
                "Incident update request processed", HttpStatus.OK);
    }
}
