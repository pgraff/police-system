package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.locations.CreateLocationCommand;
import com.knowit.policesystem.edge.commands.locations.LinkLocationToIncidentCommand;
import com.knowit.policesystem.edge.commands.locations.LinkLocationToCallCommand;
import com.knowit.policesystem.edge.commands.locations.UnlinkLocationFromIncidentCommand;
import com.knowit.policesystem.edge.commands.locations.UnlinkLocationFromCallCommand;
import com.knowit.policesystem.edge.commands.locations.UpdateLocationCommand;
import com.knowit.policesystem.edge.dto.CreateLocationRequestDto;
import com.knowit.policesystem.edge.dto.LinkLocationRequestDto;
import com.knowit.policesystem.edge.dto.LocationResponseDto;
import com.knowit.policesystem.edge.dto.UpdateLocationRequestDto;
import com.knowit.policesystem.edge.validation.locations.CreateLocationCommandValidator;
import com.knowit.policesystem.edge.validation.locations.LinkLocationToIncidentCommandValidator;
import com.knowit.policesystem.edge.validation.locations.LinkLocationToCallCommandValidator;
import com.knowit.policesystem.edge.validation.locations.UnlinkLocationFromIncidentCommandValidator;
import com.knowit.policesystem.edge.validation.locations.UnlinkLocationFromCallCommandValidator;
import com.knowit.policesystem.edge.validation.locations.UpdateLocationCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for location operations.
 * Handles HTTP requests for location-related endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class LocationController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final CreateLocationCommandValidator createCommandValidator;
    private final UpdateLocationCommandValidator updateCommandValidator;
    private final LinkLocationToIncidentCommandValidator linkLocationToIncidentCommandValidator;
    private final UnlinkLocationFromIncidentCommandValidator unlinkLocationFromIncidentCommandValidator;
    private final LinkLocationToCallCommandValidator linkLocationToCallCommandValidator;
    private final UnlinkLocationFromCallCommandValidator unlinkLocationFromCallCommandValidator;

    /**
     * Creates a new location controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param createCommandValidator the create command validator
     * @param updateCommandValidator the update command validator
     * @param linkLocationToIncidentCommandValidator the link location to incident command validator
     * @param unlinkLocationFromIncidentCommandValidator the unlink location from incident command validator
     */
    public LocationController(CommandHandlerRegistry commandHandlerRegistry,
                             CreateLocationCommandValidator createCommandValidator,
                             UpdateLocationCommandValidator updateCommandValidator,
                             LinkLocationToIncidentCommandValidator linkLocationToIncidentCommandValidator,
                             UnlinkLocationFromIncidentCommandValidator unlinkLocationFromIncidentCommandValidator,
                             LinkLocationToCallCommandValidator linkLocationToCallCommandValidator,
                             UnlinkLocationFromCallCommandValidator unlinkLocationFromCallCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.createCommandValidator = createCommandValidator;
        this.updateCommandValidator = updateCommandValidator;
        this.linkLocationToIncidentCommandValidator = linkLocationToIncidentCommandValidator;
        this.unlinkLocationFromIncidentCommandValidator = unlinkLocationFromIncidentCommandValidator;
        this.linkLocationToCallCommandValidator = linkLocationToCallCommandValidator;
        this.unlinkLocationFromCallCommandValidator = unlinkLocationFromCallCommandValidator;
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

        CreateLocationCommand command = new CreateLocationCommand(requestDto.getLocationId(), requestDto);
        return executeCommand(command, createCommandValidator, commandHandlerRegistry, CreateLocationCommand.class,
                "Location creation request created", HttpStatus.CREATED);
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

        UpdateLocationCommand command = new UpdateLocationCommand(locationId, requestDto);
        return executeCommand(command, updateCommandValidator, commandHandlerRegistry, UpdateLocationCommand.class,
                "Location update request processed", HttpStatus.OK);
    }

    /**
     * Links a location to an incident.
     * Accepts location link data, validates it, and publishes a LinkLocationToIncidentRequested event to Kafka.
     *
     * @param incidentId the incident ID from the path
     * @param requestDto the location link request DTO
     * @return 200 OK with location ID
     */
    @PostMapping("/incidents/{incidentId}/locations")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<LocationResponseDto>> linkLocationToIncident(
            @PathVariable String incidentId,
            @Valid @RequestBody LinkLocationRequestDto requestDto) {

        LinkLocationToIncidentCommand command = new LinkLocationToIncidentCommand(requestDto.getLocationId(), incidentId, requestDto);
        return executeCommand(command, linkLocationToIncidentCommandValidator, commandHandlerRegistry, LinkLocationToIncidentCommand.class,
                "Location link request processed", HttpStatus.OK);
    }

    /**
     * Links a location to a call.
     * Accepts location link data, validates it, and publishes a LinkLocationToCallRequested event to Kafka.
     *
     * @param callId the call ID from the path
     * @param requestDto the location link request DTO
     * @return 200 OK with location ID
     */
    @PostMapping("/calls/{callId}/locations")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<LocationResponseDto>> linkLocationToCall(
            @PathVariable String callId,
            @Valid @RequestBody LinkLocationRequestDto requestDto) {

        LinkLocationToCallCommand command = new LinkLocationToCallCommand(requestDto.getLocationId(), callId, requestDto);
        return executeCommand(command, linkLocationToCallCommandValidator, commandHandlerRegistry, LinkLocationToCallCommand.class,
                "Location link request processed", HttpStatus.OK);
    }

    /**
     * Unlinks a location from an incident.
     * Accepts path parameters, validates them, and publishes an UnlinkLocationFromIncidentRequested event to Kafka.
     *
     * @param incidentId the incident ID from the path
     * @param locationId the location ID from the path
     * @return 200 OK with location ID
     */
    @DeleteMapping("/incidents/{incidentId}/locations/{locationId}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<LocationResponseDto>> unlinkLocationFromIncident(
            @PathVariable String incidentId,
            @PathVariable String locationId) {

        UnlinkLocationFromIncidentCommand command = new UnlinkLocationFromIncidentCommand(locationId, incidentId, locationId);
        return executeCommand(command, unlinkLocationFromIncidentCommandValidator, commandHandlerRegistry, UnlinkLocationFromIncidentCommand.class,
                "Location unlink request processed", HttpStatus.OK);
    }

    /**
     * Unlinks a location from a call.
     * Accepts path parameters, validates them, and publishes an UnlinkLocationFromCallRequested event to Kafka.
     *
     * @param callId the call ID from the path
     * @param locationId the location ID from the path
     * @return 200 OK with location ID
     */
    @DeleteMapping("/calls/{callId}/locations/{locationId}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<LocationResponseDto>> unlinkLocationFromCall(
            @PathVariable String callId,
            @PathVariable String locationId) {

        UnlinkLocationFromCallCommand command = new UnlinkLocationFromCallCommand(locationId, callId, locationId);
        return executeCommand(command, unlinkLocationFromCallCommandValidator, commandHandlerRegistry, UnlinkLocationFromCallCommand.class,
                "Location unlink request processed", HttpStatus.OK);
    }
}
