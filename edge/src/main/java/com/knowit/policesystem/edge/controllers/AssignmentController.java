package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.assignments.CreateAssignmentCommand;
import com.knowit.policesystem.edge.commands.assignments.CompleteAssignmentCommand;
import com.knowit.policesystem.edge.commands.assignments.ChangeAssignmentStatusCommand;
import com.knowit.policesystem.edge.commands.assignments.LinkAssignmentToDispatchCommand;
import com.knowit.policesystem.edge.commands.assignments.AssignResourceCommand;
import com.knowit.policesystem.edge.commands.assignments.UnassignResourceCommand;
import com.knowit.policesystem.edge.dto.AssignmentResponseDto;
import com.knowit.policesystem.edge.dto.CreateAssignmentRequestDto;
import com.knowit.policesystem.edge.dto.CompleteAssignmentRequestDto;
import com.knowit.policesystem.edge.dto.ChangeAssignmentStatusRequestDto;
import com.knowit.policesystem.edge.dto.LinkAssignmentToDispatchRequestDto;
import com.knowit.policesystem.edge.dto.LinkAssignmentToDispatchResponseDto;
import com.knowit.policesystem.edge.dto.AssignResourceRequestDto;
import com.knowit.policesystem.edge.dto.UnassignResourceRequestDto;
import com.knowit.policesystem.edge.dto.ResourceAssignmentResponseDto;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.assignments.CreateAssignmentCommandValidator;
import com.knowit.policesystem.edge.validation.assignments.CompleteAssignmentCommandValidator;
import com.knowit.policesystem.edge.validation.assignments.ChangeAssignmentStatusCommandValidator;
import com.knowit.policesystem.edge.validation.assignments.LinkAssignmentToDispatchCommandValidator;
import com.knowit.policesystem.edge.validation.assignments.AssignResourceCommandValidator;
import com.knowit.policesystem.edge.validation.assignments.UnassignResourceCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final ChangeAssignmentStatusCommandValidator changeAssignmentStatusCommandValidator;
    private final LinkAssignmentToDispatchCommandValidator linkAssignmentToDispatchCommandValidator;
    private final AssignResourceCommandValidator assignResourceCommandValidator;
    private final UnassignResourceCommandValidator unassignResourceCommandValidator;

    /**
     * Creates a new assignment controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param commandValidator the create assignment command validator
     * @param completeAssignmentCommandValidator the complete assignment command validator
     * @param changeAssignmentStatusCommandValidator the change assignment status command validator
     * @param linkAssignmentToDispatchCommandValidator the link assignment to dispatch command validator
     * @param assignResourceCommandValidator the assign resource command validator
     * @param unassignResourceCommandValidator the unassign resource command validator
     */
    public AssignmentController(CommandHandlerRegistry commandHandlerRegistry,
                               CreateAssignmentCommandValidator commandValidator,
                               CompleteAssignmentCommandValidator completeAssignmentCommandValidator,
                               ChangeAssignmentStatusCommandValidator changeAssignmentStatusCommandValidator,
                               LinkAssignmentToDispatchCommandValidator linkAssignmentToDispatchCommandValidator,
                               AssignResourceCommandValidator assignResourceCommandValidator,
                               UnassignResourceCommandValidator unassignResourceCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandValidator = commandValidator;
        this.completeAssignmentCommandValidator = completeAssignmentCommandValidator;
        this.changeAssignmentStatusCommandValidator = changeAssignmentStatusCommandValidator;
        this.linkAssignmentToDispatchCommandValidator = linkAssignmentToDispatchCommandValidator;
        this.assignResourceCommandValidator = assignResourceCommandValidator;
        this.unassignResourceCommandValidator = unassignResourceCommandValidator;
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

    /**
     * Changes an assignment's status.
     * Accepts status change data, validates it, and publishes a ChangeAssignmentStatusRequested event to Kafka.
     *
     * @param assignmentId the assignment identifier from the path
     * @param requestDto the change assignment status request DTO
     * @return 200 OK with assignment ID
     */
    @PatchMapping("/assignments/{assignmentId}/status")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<AssignmentResponseDto>> changeAssignmentStatus(
            @PathVariable String assignmentId,
            @Valid @RequestBody ChangeAssignmentStatusRequestDto requestDto) {

        // Create command from path variable and DTO
        ChangeAssignmentStatusCommand command = new ChangeAssignmentStatusCommand(assignmentId, requestDto);

        // Validate command
        var validationResult = changeAssignmentStatusCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<ChangeAssignmentStatusCommand, AssignmentResponseDto> handler =
                commandHandlerRegistry.findHandler(ChangeAssignmentStatusCommand.class);
        AssignmentResponseDto response = handler.handle(command);

        // Return 200 OK response
        return success(response, "Assignment status change request processed");
    }

    /**
     * Links an assignment to a dispatch.
     * Accepts dispatch link data, validates it, and publishes a LinkAssignmentToDispatchRequested event to Kafka.
     *
     * @param assignmentId the assignment identifier from the path
     * @param requestDto the link assignment to dispatch request DTO
     * @return 200 OK with assignmentId and dispatchId
     */
    @PostMapping("/assignments/{assignmentId}/dispatches")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<LinkAssignmentToDispatchResponseDto>> linkAssignmentToDispatch(
            @PathVariable String assignmentId,
            @Valid @RequestBody LinkAssignmentToDispatchRequestDto requestDto) {

        // Create command from DTO and path parameter
        LinkAssignmentToDispatchCommand command = new LinkAssignmentToDispatchCommand(assignmentId, assignmentId, requestDto);

        // Validate command
        var validationResult = linkAssignmentToDispatchCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<LinkAssignmentToDispatchCommand, LinkAssignmentToDispatchResponseDto> handler =
                commandHandlerRegistry.findHandler(LinkAssignmentToDispatchCommand.class);
        LinkAssignmentToDispatchResponseDto response = handler.handle(command);

        // Return 200 OK response
        return success(response, "Assignment link request processed");
    }

    /**
     * Assigns a resource to an assignment.
     * Accepts resource assignment data, validates it, and publishes an AssignResourceRequested event to Kafka.
     *
     * @param assignmentId the assignment identifier from the path
     * @param requestDto the assign resource request DTO
     * @return 201 Created with resource assignment ID
     */
    @PostMapping("/assignments/{assignmentId}/resources")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<ResourceAssignmentResponseDto>> assignResource(
            @PathVariable String assignmentId,
            @Valid @RequestBody AssignResourceRequestDto requestDto) {

        // Create command from path variable and DTO
        AssignResourceCommand command = new AssignResourceCommand(assignmentId, assignmentId, requestDto);

        // Validate command
        var validationResult = assignResourceCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<AssignResourceCommand, ResourceAssignmentResponseDto> handler =
                commandHandlerRegistry.findHandler(AssignResourceCommand.class);
        ResourceAssignmentResponseDto response = handler.handle(command);

        // Return 201 Created response
        return created(response, "Resource assignment request processed");
    }

    /**
     * Unassigns a resource from an assignment.
     * Accepts resource unassignment data, validates it, and publishes an UnassignResourceRequested event to Kafka.
     *
     * @param assignmentId the assignment identifier from the path
     * @param resourceId the resource identifier from the path
     * @param requestDto the unassign resource request DTO
     * @return 200 OK with resource assignment ID
     */
    @DeleteMapping("/assignments/{assignmentId}/resources/{resourceId}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<ResourceAssignmentResponseDto>> unassignResource(
            @PathVariable String assignmentId,
            @PathVariable String resourceId,
            @Valid @RequestBody UnassignResourceRequestDto requestDto) {

        // Create command from path variables and DTO
        UnassignResourceCommand command = new UnassignResourceCommand(assignmentId, assignmentId, resourceId, requestDto);

        // Validate command
        var validationResult = unassignResourceCommandValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        com.knowit.policesystem.edge.commands.CommandHandler<UnassignResourceCommand, ResourceAssignmentResponseDto> handler =
                commandHandlerRegistry.findHandler(UnassignResourceCommand.class);
        ResourceAssignmentResponseDto response = handler.handle(command);

        // Return 200 OK response
        return success(response, "Resource unassignment request processed");
    }
}
