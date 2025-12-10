package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.assignments.CreateAssignmentCommand;
import com.knowit.policesystem.edge.commands.assignments.CompleteAssignmentCommand;
import com.knowit.policesystem.edge.dto.AssignmentResponseDto;
import com.knowit.policesystem.edge.dto.CreateAssignmentRequestDto;
import com.knowit.policesystem.edge.dto.CompleteAssignmentRequestDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.assignments.CreateAssignmentCommandValidator;
import com.knowit.policesystem.edge.validation.assignments.CompleteAssignmentCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for assignment operations.
 * Handles HTTP requests for assignment-related endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class AssignmentController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final CreateAssignmentCommandValidator commandValidator;
    private final CompleteAssignmentCommandValidator completeAssignmentCommandValidator;

    /**
     * Creates a new assignment controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the create assignment command validator
     * @param completeAssignmentCommandValidator the complete assignment command validator
     */
    public AssignmentController(CommandHandlerRegistry commandHandlerRegistry,
                               CreateAssignmentCommandValidator commandValidator,
                               CompleteAssignmentCommandValidator completeAssignmentCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
        this.completeAssignmentCommandValidator = completeAssignmentCommandValidator;
    }

    /**
     * Creates a new assignment.
     * Accepts assignment data, validates it, and publishes a CreateAssignmentRequested event to Kafka.
     *
     * @param requestDto the create assignment request DTO
     * @return 201 Created with assignment ID
     */
    @PostMapping("/assignments")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<AssignmentResponseDto>> createAssignment(
            @Valid @RequestBody CreateAssignmentRequestDto requestDto) {

        // Create command from DTO
        CreateAssignmentCommand command = new CreateAssignmentCommand(requestDto.getAssignmentId(), requestDto);

        // Validate command
        var validationResult = commandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<CreateAssignmentCommand, AssignmentResponseDto> handler =
                commandHandlerRegistry.findHandler(CreateAssignmentCommand.class);
        AssignmentResponseDto response = handler.handle(command);

        // Return 201 Created response
        return created(response, "Assignment creation request processed");
    }

    /**
     * Completes an assignment.
     * Accepts completion data, validates it, and publishes a CompleteAssignmentRequested event to Kafka.
     *
     * @param assignmentId the assignment identifier from the path
     * @param requestDto the complete assignment request DTO
     * @return 200 OK with assignment ID
     */
    @PostMapping("/assignments/{assignmentId}/complete")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<AssignmentResponseDto>> completeAssignment(
            @PathVariable String assignmentId,
            @Valid @RequestBody CompleteAssignmentRequestDto requestDto) {

        // Create command from path variable and DTO
        CompleteAssignmentCommand command = new CompleteAssignmentCommand(assignmentId, requestDto);

        // Validate command
        var validationResult = completeAssignmentCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<CompleteAssignmentCommand, AssignmentResponseDto> handler =
                commandHandlerRegistry.findHandler(CompleteAssignmentCommand.class);
        AssignmentResponseDto response = handler.handle(command);

        // Return 200 OK response
        return success(response, "Assignment completion request processed");
    }
}
