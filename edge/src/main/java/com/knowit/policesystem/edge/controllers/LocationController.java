package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.locations.CreateLocationCommand;
import com.knowit.policesystem.edge.dto.CreateLocationRequestDto;
import com.knowit.policesystem.edge.dto.LocationResponseDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.locations.CreateLocationCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for location operations.
 * Handles HTTP requests for location-related endpoints.
 */
@RestController
public class LocationController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final CreateLocationCommandValidator createCommandValidator;

    /**
     * Creates a new location controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param createCommandValidator the create command validator
     */
    public LocationController(CommandHandlerRegistry commandHandlerRegistry,
                             CreateLocationCommandValidator createCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.createCommandValidator = createCommandValidator;
    }

    /**
     * Creates a new location.
     * Accepts location data, validates it, and publishes a CreateLocationRequested event to Kafka and NATS/JetStream.
     *
     * @param requestDto the location creation request DTO
     * @return 201 Created with location ID
     */
    @PostMapping("/locations")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<LocationResponseDto>> createLocation(
            @Valid @RequestBody CreateLocationRequestDto requestDto) {

        // Create command from DTO
        CreateLocationCommand command = new CreateLocationCommand(requestDto.getLocationId(), requestDto);

        // Validate command
        var validationResult = createCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<CreateLocationCommand, LocationResponseDto> handler =
                commandHandlerRegistry.findHandler(CreateLocationCommand.class);
        LocationResponseDto response = handler.handle(command);

        // Return 201 Created response
        return created(response, "Location creation request created");
    }
}
