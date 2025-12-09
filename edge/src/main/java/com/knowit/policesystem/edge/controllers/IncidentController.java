package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.incidents.ArriveAtIncidentCommand;
import com.knowit.policesystem.edge.commands.incidents.DispatchIncidentCommand;
import com.knowit.policesystem.edge.commands.incidents.ReportIncidentCommand;
import com.knowit.policesystem.edge.dto.ArriveAtIncidentRequestDto;
import com.knowit.policesystem.edge.dto.DispatchIncidentRequestDto;
import com.knowit.policesystem.edge.dto.IncidentResponseDto;
import com.knowit.policesystem.edge.dto.ReportIncidentRequestDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.incidents.ArriveAtIncidentCommandValidator;
import com.knowit.policesystem.edge.validation.incidents.DispatchIncidentCommandValidator;
import com.knowit.policesystem.edge.validation.incidents.ReportIncidentCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for incident operations.
 * Handles HTTP requests for incident-related endpoints.
 */
@RestController
public class IncidentController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final ReportIncidentCommandValidator commandValidator;
    private final DispatchIncidentCommandValidator dispatchCommandValidator;
    private final ArriveAtIncidentCommandValidator arriveCommandValidator;

    /**
     * Creates a new incident controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the report incident command validator
     * @param dispatchCommandValidator the dispatch incident command validator
     */
    public IncidentController(CommandHandlerRegistry commandHandlerRegistry,
                              ReportIncidentCommandValidator commandValidator,
                              DispatchIncidentCommandValidator dispatchCommandValidator,
                              ArriveAtIncidentCommandValidator arriveCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
        this.dispatchCommandValidator = dispatchCommandValidator;
        this.arriveCommandValidator = arriveCommandValidator;
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

        // Create command from DTO
        ReportIncidentCommand command = new ReportIncidentCommand(requestDto.getIncidentId(), requestDto);

        // Validate command
        var validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<ReportIncidentCommand, IncidentResponseDto> handler =
                commandHandlerRegistry.findHandler(ReportIncidentCommand.class);
        IncidentResponseDto response = handler.handle(command);

        // Return 201 Created response
        return created(response, "Incident report request created");
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

        var validationResult = dispatchCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        com.knowit.policesystem.edge.commands.CommandHandler<DispatchIncidentCommand, IncidentResponseDto> handler =
                commandHandlerRegistry.findHandler(DispatchIncidentCommand.class);
        IncidentResponseDto response = handler.handle(command);

        return success(response, "Incident dispatch request created");
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

        var validationResult = arriveCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        com.knowit.policesystem.edge.commands.CommandHandler<ArriveAtIncidentCommand, IncidentResponseDto> handler =
                commandHandlerRegistry.findHandler(ArriveAtIncidentCommand.class);
        IncidentResponseDto response = handler.handle(command);

        return success(response, "Incident arrival request created");
    }
}
