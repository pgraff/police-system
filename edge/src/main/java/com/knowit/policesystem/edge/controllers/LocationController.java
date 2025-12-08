package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.locations.CreateLocationCommand;
import com.knowit.policesystem.edge.commands.locations.UpdateLocationCommand;
import com.knowit.policesystem.edge.dto.CreateLocationRequestDto;
import com.knowit.policesystem.edge.dto.LocationResponseDto;
import com.knowit.policesystem.edge.dto.UpdateLocationRequestDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.locations.CreateLocationCommandValidator;
import com.knowit.policesystem.edge.validation.locations.UpdateLocationCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final UpdateLocationCommandValidator updateCommandValidator;

    /**
     * Creates a new location controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param createCommandValidator the create command validator
     * @param updateCommandValidator the update command validator
     */
    public LocationController(CommandHandlerRegistry commandHandlerRegistry,
                             CreateLocationCommandValidator createCommandValidator,
                             UpdateLocationCommandValidator updateCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.createCommandValidator = createCommandValidator;
        this.updateCommandValidator = updateCommandValidator;
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

    /**
     * Updates an existing location.
     * Accepts location update data, validates it, and publishes an UpdateLocationRequested event to Kafka and NATS/JetStream.
     *
     * @param locationId the location ID from the path
     * @param requestDto the location update request DTO
     * @return 200 OK with location ID
     */
    @PutMapping("/locations/{locationId}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<LocationResponseDto>> updateLocation(
            @PathVariable String locationId,
            @Valid @RequestBody UpdateLocationRequestDto requestDto) {

        // Create command from DTO
        UpdateLocationCommand command = new UpdateLocationCommand(locationId, requestDto);

        // Validate command
        var validationResult = updateCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<UpdateLocationCommand, LocationResponseDto> handler =
                commandHandlerRegistry.findHandler(UpdateLocationCommand.class);
        LocationResponseDto response = handler.handle(command);

        // Return 200 OK response
        return success(response, "Location update request processed");
    }
}
