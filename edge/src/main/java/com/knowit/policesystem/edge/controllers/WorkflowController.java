package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.commands.workflows.IncidentDispatchWorkflowCommand;
import com.knowit.policesystem.edge.dto.IncidentDispatchWorkflowRequestDto;
import com.knowit.policesystem.edge.dto.IncidentDispatchWorkflowResponseDto;
import com.knowit.policesystem.edge.validation.workflows.IncidentDispatchWorkflowCommandValidator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for workflow operations.
 * Provides high-level endpoints that orchestrate multiple operations in one call.
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController extends BaseRestController {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final IncidentDispatchWorkflowCommandValidator workflowCommandValidator;

    /**
     * Creates a new workflow controller.
     *
     * @param commandHandlerRegistry the command handler registry
     * @param workflowCommandValidator the workflow command validator
     */
    public WorkflowController(CommandHandlerRegistry commandHandlerRegistry,
                             IncidentDispatchWorkflowCommandValidator workflowCommandValidator) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.workflowCommandValidator = workflowCommandValidator;
    }

    /**
     * Executes an incident dispatch workflow.
     * Orchestrates: create incident, link location/calls, dispatch, create assignment, assign resources.
     * All operations are performed in one API call for UI convenience.
     *
     * @param requestDto the workflow request DTO
     * @return 201 Created with all created resource IDs
     */
    @PostMapping("/incident-dispatch")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<IncidentDispatchWorkflowResponseDto>> incidentDispatch(
            @Valid @RequestBody IncidentDispatchWorkflowRequestDto requestDto) {

        String incidentId = requestDto.getIncident().getIncidentId();
        IncidentDispatchWorkflowCommand command = new IncidentDispatchWorkflowCommand(incidentId, requestDto);
        
        return executeCommand(command, workflowCommandValidator, commandHandlerRegistry,
                IncidentDispatchWorkflowCommand.class, "Incident dispatch workflow completed", HttpStatus.CREATED);
    }
}
