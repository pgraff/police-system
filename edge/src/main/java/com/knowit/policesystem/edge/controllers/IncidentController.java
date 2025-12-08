package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.incidents.ReportIncidentCommand;
import com.knowit.policesystem.edge.dto.IncidentResponseDto;
import com.knowit.policesystem.edge.dto.ReportIncidentRequestDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.incidents.ReportIncidentCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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

    /**
     * Creates a new incident controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the command validator
     */
    public IncidentController(CommandHandlerRegistry commandHandlerRegistry,
                              ReportIncidentCommandValidator commandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
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
}
