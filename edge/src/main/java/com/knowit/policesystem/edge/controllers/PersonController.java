package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.persons.RegisterPersonCommand;
import com.knowit.policesystem.edge.commands.persons.UpdatePersonCommand;
import com.knowit.policesystem.edge.dto.PersonResponseDto;
import com.knowit.policesystem.edge.dto.RegisterPersonRequestDto;
import com.knowit.policesystem.edge.dto.UpdatePersonRequestDto;
import com.knowit.policesystem.edge.validation.persons.RegisterPersonCommandValidator;
import com.knowit.policesystem.edge.validation.persons.UpdatePersonCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for person operations.
 * Handles HTTP requests for person-related endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class PersonController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final RegisterPersonCommandValidator registerCommandValidator;
    private final UpdatePersonCommandValidator updateCommandValidator;

    /**
     * Creates a new person controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param registerCommandValidator the register command validator
     * @param updateCommandValidator the update command validator
     */
    public PersonController(CommandHandlerRegistry commandHandlerRegistry,
                            RegisterPersonCommandValidator registerCommandValidator,
                            UpdatePersonCommandValidator updateCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.registerCommandValidator = registerCommandValidator;
        this.updateCommandValidator = updateCommandValidator;
    }

    /**
     * Registers a new person.
     * Accepts person data, validates it, and publishes a RegisterPersonRequested event to Kafka and NATS/JetStream.
     *
     * @param requestDto the person registration request DTO
     * @return 201 Created with person ID
     */
    @PostMapping("/persons")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<PersonResponseDto>> registerPerson(
            @Valid @RequestBody RegisterPersonRequestDto requestDto) {

        RegisterPersonCommand command = new RegisterPersonCommand(requestDto.getPersonId(), requestDto);
        return executeCommand(command, registerCommandValidator, commandHandlerRegistry, RegisterPersonCommand.class,
                "Person registration request created", HttpStatus.CREATED);
    }

    /**
     * Updates a person.
     * Accepts person update data, validates it, and publishes an UpdatePersonRequested event to Kafka and NATS/JetStream.
     *
     * @param personId the person ID
     * @param requestDto the person update request DTO
     * @return 200 OK with person ID
     */
    @PutMapping("/persons/{personId}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<PersonResponseDto>> updatePerson(
            @PathVariable String personId,
            @Valid @RequestBody UpdatePersonRequestDto requestDto) {

        UpdatePersonCommand command = new UpdatePersonCommand(personId, requestDto);
        return executeCommand(command, updateCommandValidator, commandHandlerRegistry, UpdatePersonCommand.class,
                "Person update request processed", HttpStatus.OK);
    }
}
