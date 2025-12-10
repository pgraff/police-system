package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.activities.ChangeActivityStatusCommand;
import com.knowit.policesystem.edge.commands.activities.CompleteActivityCommand;
import com.knowit.policesystem.edge.commands.activities.LinkActivityToIncidentCommand;
import com.knowit.policesystem.edge.commands.activities.StartActivityCommand;
import com.knowit.policesystem.edge.commands.activities.UpdateActivityCommand;
import com.knowit.policesystem.edge.dto.ActivityResponseDto;
import com.knowit.policesystem.edge.dto.ChangeActivityStatusRequestDto;
import com.knowit.policesystem.edge.dto.CompleteActivityRequestDto;
import com.knowit.policesystem.edge.dto.LinkActivityToIncidentRequestDto;
import com.knowit.policesystem.edge.dto.LinkActivityToIncidentResponseDto;
import com.knowit.policesystem.edge.dto.StartActivityRequestDto;
import com.knowit.policesystem.edge.dto.UpdateActivityRequestDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.activities.ChangeActivityStatusCommandValidator;
import com.knowit.policesystem.edge.validation.activities.CompleteActivityCommandValidator;
import com.knowit.policesystem.edge.validation.activities.LinkActivityToIncidentCommandValidator;
import com.knowit.policesystem.edge.validation.activities.StartActivityCommandValidator;
import com.knowit.policesystem.edge.validation.activities.UpdateActivityCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for activity operations.
 * Handles HTTP requests for activity-related endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class ActivityController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final StartActivityCommandValidator commandValidator;
    private final CompleteActivityCommandValidator completeActivityCommandValidator;
    private final ChangeActivityStatusCommandValidator changeActivityStatusCommandValidator;
    private final UpdateActivityCommandValidator updateActivityCommandValidator;
    private final LinkActivityToIncidentCommandValidator linkActivityToIncidentCommandValidator;

    /**
     * Creates a new activity controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the start activity command validator
     * @param completeActivityCommandValidator the complete activity command validator
     * @param changeActivityStatusCommandValidator the change activity status command validator
     * @param updateActivityCommandValidator the update activity command validator
     * @param linkActivityToIncidentCommandValidator the link activity to incident command validator
     */
    public ActivityController(CommandHandlerRegistry commandHandlerRegistry,
                              StartActivityCommandValidator commandValidator,
                              CompleteActivityCommandValidator completeActivityCommandValidator,
                              ChangeActivityStatusCommandValidator changeActivityStatusCommandValidator,
                              UpdateActivityCommandValidator updateActivityCommandValidator,
                              LinkActivityToIncidentCommandValidator linkActivityToIncidentCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
        this.completeActivityCommandValidator = completeActivityCommandValidator;
        this.changeActivityStatusCommandValidator = changeActivityStatusCommandValidator;
        this.updateActivityCommandValidator = updateActivityCommandValidator;
        this.linkActivityToIncidentCommandValidator = linkActivityToIncidentCommandValidator;
    }

    /**
     * Starts a new activity.
     * Accepts activity data, validates it, and publishes a StartActivityRequested event to Kafka.
     *
     * @param requestDto the start activity request DTO
     * @return 201 Created with activity ID
     */
    @PostMapping("/activities")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<ActivityResponseDto>> startActivity(
            @Valid @RequestBody StartActivityRequestDto requestDto) {

        // Create command from DTO
        StartActivityCommand command = new StartActivityCommand(requestDto.getActivityId(), requestDto);

        // Validate command
        var validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<StartActivityCommand, ActivityResponseDto> handler =
                commandHandlerRegistry.findHandler(StartActivityCommand.class);
        ActivityResponseDto response = handler.handle(command);

        // Return 201 Created response
        return created(response, "Activity start request created");
    }

    /**
     * Completes an activity.
     * Accepts completion data, validates it, and publishes a CompleteActivityRequested event to Kafka.
     *
     * @param activityId the activity identifier from the path
     * @param requestDto the complete activity request DTO
     * @return 200 OK with activity ID
     */
    @PostMapping("/activities/{activityId}/complete")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<ActivityResponseDto>> completeActivity(
            @PathVariable String activityId,
            @Valid @RequestBody CompleteActivityRequestDto requestDto) {

        // Create command from path variable and DTO
        CompleteActivityCommand command = new CompleteActivityCommand(activityId, requestDto);

        // Validate command
        var validationResult = completeActivityCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<CompleteActivityCommand, ActivityResponseDto> handler =
                commandHandlerRegistry.findHandler(CompleteActivityCommand.class);
        ActivityResponseDto response = handler.handle(command);

        // Return 200 OK response
        return success(response, "Activity completion request processed");
    }

    /**
     * Changes an activity's status.
     * Accepts status change data, validates it, and publishes a ChangeActivityStatusRequested event to Kafka.
     *
     * @param activityId the activity identifier from the path
     * @param requestDto the change activity status request DTO
     * @return 200 OK with activity ID
     */
    @PatchMapping("/activities/{activityId}/status")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<ActivityResponseDto>> changeActivityStatus(
            @PathVariable String activityId,
            @Valid @RequestBody ChangeActivityStatusRequestDto requestDto) {

        // Create command from path variable and DTO
        ChangeActivityStatusCommand command = new ChangeActivityStatusCommand(activityId, requestDto);

        // Validate command
        var validationResult = changeActivityStatusCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<ChangeActivityStatusCommand, ActivityResponseDto> handler =
                commandHandlerRegistry.findHandler(ChangeActivityStatusCommand.class);
        ActivityResponseDto response = handler.handle(command);

        // Return 200 OK response
        return success(response, "Activity status change request processed");
    }

    /**
     * Updates an activity's details.
     * Accepts optional description, validates it, and publishes an UpdateActivityRequested event to Kafka.
     *
     * @param activityId the activity identifier from the path
     * @param requestDto the update activity request DTO
     * @return 200 OK with activity ID
     */
    @PutMapping("/activities/{activityId}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<ActivityResponseDto>> updateActivity(
            @PathVariable String activityId,
            @Valid @RequestBody UpdateActivityRequestDto requestDto) {

        UpdateActivityCommand command = new UpdateActivityCommand(activityId, requestDto);

        var validationResult = updateActivityCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        com.knowit.policesystem.edge.commands.CommandHandler<UpdateActivityCommand, ActivityResponseDto> handler =
                commandHandlerRegistry.findHandler(UpdateActivityCommand.class);
        ActivityResponseDto response = handler.handle(command);

        return success(response, "Activity update request processed");
    }

    /**
     * Links an activity to an incident.
     * Accepts incident link data, validates it, and publishes a LinkActivityToIncidentRequested event to Kafka.
     *
     * @param activityId the activity ID from the path
     * @param requestDto the incident link request DTO
     * @return 200 OK with activityId and incidentId
     */
    @PostMapping("/activities/{activityId}/incidents")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<LinkActivityToIncidentResponseDto>> linkActivityToIncident(
            @PathVariable String activityId,
            @Valid @RequestBody LinkActivityToIncidentRequestDto requestDto) {

        // Create command from DTO and path parameter
        LinkActivityToIncidentCommand command = new LinkActivityToIncidentCommand(activityId, activityId, requestDto);

        // Validate command
        var validationResult = linkActivityToIncidentCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<LinkActivityToIncidentCommand, LinkActivityToIncidentResponseDto> handler =
                commandHandlerRegistry.findHandler(LinkActivityToIncidentCommand.class);
        LinkActivityToIncidentResponseDto response = handler.handle(command);

        // Return 200 OK response
        return success(response, "Activity link request processed");
    }
}
