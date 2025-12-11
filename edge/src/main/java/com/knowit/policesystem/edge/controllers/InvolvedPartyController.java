package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.involvedparties.EndPartyInvolvementCommand;
import com.knowit.policesystem.edge.commands.involvedparties.InvolvePartyCommand;
import com.knowit.policesystem.edge.commands.involvedparties.UpdatePartyInvolvementCommand;
import com.knowit.policesystem.edge.dto.EndPartyInvolvementRequestDto;
import com.knowit.policesystem.edge.dto.InvolvePartyRequestDto;
import com.knowit.policesystem.edge.dto.UpdatePartyInvolvementRequestDto;
import com.knowit.policesystem.edge.dto.InvolvementResponseDto;
import com.knowit.policesystem.edge.validation.involvedparties.EndPartyInvolvementCommandValidator;
import com.knowit.policesystem.edge.validation.involvedparties.InvolvePartyCommandValidator;
import com.knowit.policesystem.edge.validation.involvedparties.UpdatePartyInvolvementCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final UpdatePartyInvolvementCommandValidator updatePartyInvolvementCommandValidator;

    /**
     * Creates a new involved party controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the command validator
     * @param endPartyInvolvementCommandValidator the end party involvement command validator
     * @param updatePartyInvolvementCommandValidator the update party involvement command validator
     */
    public InvolvedPartyController(CommandHandlerRegistry commandHandlerRegistry,
                                   InvolvePartyCommandValidator commandValidator,
                                   EndPartyInvolvementCommandValidator endPartyInvolvementCommandValidator,
                                   UpdatePartyInvolvementCommandValidator updatePartyInvolvementCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
        this.endPartyInvolvementCommandValidator = endPartyInvolvementCommandValidator;
        this.updatePartyInvolvementCommandValidator = updatePartyInvolvementCommandValidator;
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

        String involvementId = UUID.randomUUID().toString();
        InvolvePartyCommand command = new InvolvePartyCommand(involvementId, requestDto);
        return executeCommand(command, commandValidator, commandHandlerRegistry, InvolvePartyCommand.class,
                "Party involvement request processed", HttpStatus.CREATED);
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

        EndPartyInvolvementCommand command = new EndPartyInvolvementCommand(involvementId, requestDto);
        return executeCommand(command, endPartyInvolvementCommandValidator, commandHandlerRegistry, EndPartyInvolvementCommand.class,
                "Party involvement end request processed", HttpStatus.OK);
    }

    /**
     * Updates a party involvement.
     * Accepts update data (partyRoleType and/or description), validates it, and publishes an UpdatePartyInvolvementRequested event to Kafka.
     *
     * @param involvementId the involvement identifier from the path
     * @param requestDto the update party involvement request DTO
     * @return 200 OK with involvement ID
     */
    @PutMapping("/{involvementId}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<InvolvementResponseDto>> updatePartyInvolvement(
            @PathVariable String involvementId,
            @Valid @RequestBody UpdatePartyInvolvementRequestDto requestDto) {

        UpdatePartyInvolvementCommand command = new UpdatePartyInvolvementCommand(involvementId, requestDto);
        return executeCommand(command, updatePartyInvolvementCommandValidator, commandHandlerRegistry, UpdatePartyInvolvementCommand.class,
                "Party involvement update request processed", HttpStatus.OK);
    }
}
