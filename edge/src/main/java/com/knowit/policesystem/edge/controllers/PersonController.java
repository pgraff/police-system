package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.persons.RegisterPersonCommand;
import com.knowit.policesystem.edge.dto.PersonResponseDto;
import com.knowit.policesystem.edge.dto.RegisterPersonRequestDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.persons.RegisterPersonCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for person operations.
 * Handles HTTP requests for person-related endpoints.
 */
@RestController
public class PersonController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final RegisterPersonCommandValidator registerCommandValidator;

    /**
     * Creates a new person controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param registerCommandValidator the register command validator
     */
    public PersonController(CommandHandlerRegistry commandHandlerRegistry,
                            RegisterPersonCommandValidator registerCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.registerCommandValidator = registerCommandValidator;
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

        // Create command from DTO
        RegisterPersonCommand command = new RegisterPersonCommand(requestDto.getPersonId(), requestDto);

        // Validate command
        var validationResult = registerCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<RegisterPersonCommand, PersonResponseDto> handler =
                commandHandlerRegistry.findHandler(RegisterPersonCommand.class);
        PersonResponseDto response = handler.handle(command);

        // Return 201 Created response
        return created(response, "Person registration request created");
    }
}
