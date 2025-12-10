package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.activities.CompleteActivityCommand;
import com.knowit.policesystem.edge.commands.activities.StartActivityCommand;
import com.knowit.policesystem.edge.dto.ActivityResponseDto;
import com.knowit.policesystem.edge.dto.CompleteActivityRequestDto;
import com.knowit.policesystem.edge.dto.StartActivityRequestDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.activities.CompleteActivityCommandValidator;
import com.knowit.policesystem.edge.validation.activities.StartActivityCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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

    /**
     * Creates a new activity controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the start activity command validator
     * @param completeActivityCommandValidator the complete activity command validator
     */
    public ActivityController(CommandHandlerRegistry commandHandlerRegistry,
                              StartActivityCommandValidator commandValidator,
                              CompleteActivityCommandValidator completeActivityCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
        this.completeActivityCommandValidator = completeActivityCommandValidator;
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
}
