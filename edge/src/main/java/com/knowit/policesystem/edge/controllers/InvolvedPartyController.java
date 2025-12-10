package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.involvedparties.EndPartyInvolvementCommand;
import com.knowit.policesystem.edge.commands.involvedparties.InvolvePartyCommand;
import com.knowit.policesystem.edge.dto.EndPartyInvolvementRequestDto;
import com.knowit.policesystem.edge.dto.InvolvePartyRequestDto;
import com.knowit.policesystem.edge.dto.InvolvementResponseDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.involvedparties.EndPartyInvolvementCommandValidator;
import com.knowit.policesystem.edge.validation.involvedparties.InvolvePartyCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for involved party operations.
 * Handles HTTP requests for involved party-related endpoints.
 */
@RestController
@RequestMapping("/api/v1/involved-parties")
public class InvolvedPartyController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final InvolvePartyCommandValidator commandValidator;
    private final EndPartyInvolvementCommandValidator endPartyInvolvementCommandValidator;

    /**
     * Creates a new involved party controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the command validator
     * @param endPartyInvolvementCommandValidator the end party involvement command validator
     */
    public InvolvedPartyController(CommandHandlerRegistry commandHandlerRegistry,
                                   InvolvePartyCommandValidator commandValidator,
                                   EndPartyInvolvementCommandValidator endPartyInvolvementCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
        this.endPartyInvolvementCommandValidator = endPartyInvolvementCommandValidator;
    }

    /**
     * Involves a party in an incident, call, or activity.
     * Accepts involvement data, validates it, and publishes an InvolvePartyRequested event to Kafka.
     *
     * @param requestDto the involve party request DTO
     * @return 201 Created with involvement ID
     */
    @PostMapping
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<InvolvementResponseDto>> involveParty(
            @Valid @RequestBody InvolvePartyRequestDto requestDto) {

        // Generate involvementId
        String involvementId = UUID.randomUUID().toString();

        // Create command from DTO
        InvolvePartyCommand command = new InvolvePartyCommand(involvementId, requestDto);

        // Validate command
        var validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<InvolvePartyCommand, InvolvementResponseDto> handler =
                commandHandlerRegistry.findHandler(InvolvePartyCommand.class);
        InvolvementResponseDto response = handler.handle(command);

        // Return 201 Created response
        return created(response, "Party involvement request processed");
    }

    /**
     * Ends a party involvement.
     * Accepts involvement end time, validates it, and publishes an EndPartyInvolvementRequested event to Kafka.
     *
     * @param involvementId the involvement identifier from the path
     * @param requestDto the end party involvement request DTO
     * @return 200 OK with involvement ID
     */
    @PostMapping("/{involvementId}/end")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<InvolvementResponseDto>> endPartyInvolvement(
            @PathVariable String involvementId,
            @Valid @RequestBody EndPartyInvolvementRequestDto requestDto) {

        // Create command from path variable and DTO
        EndPartyInvolvementCommand command = new EndPartyInvolvementCommand(involvementId, requestDto);

        // Validate command
        var validationResult = endPartyInvolvementCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<EndPartyInvolvementCommand, InvolvementResponseDto> handler =
                commandHandlerRegistry.findHandler(EndPartyInvolvementCommand.class);
        InvolvementResponseDto response = handler.handle(command);

        // Return 200 OK response
        return success(response, "Party involvement end request processed");
    }
}
