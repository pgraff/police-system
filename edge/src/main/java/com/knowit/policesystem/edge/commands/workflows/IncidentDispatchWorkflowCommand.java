package com.knowit.policesystem.edge.commands.workflows;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.IncidentDispatchWorkflowRequestDto;

/**
 * Command for incident dispatch workflow.
 * Orchestrates multiple operations: create incident, dispatch, create assignment, assign resources.
 * Processed by IncidentDispatchWorkflowCommandHandler.
 */
public class IncidentDispatchWorkflowCommand extends Command {

    private IncidentDispatchWorkflowRequestDto requestDto;

    /**
     * Default constructor for deserialization.
     */
    public IncidentDispatchWorkflowCommand() {
        super();
    }

    /**
     * Creates a new incident dispatch workflow command.
     *
     * @param aggregateId the aggregate identifier (incidentId)
     * @param requestDto the workflow request DTO
     */
    public IncidentDispatchWorkflowCommand(String aggregateId, IncidentDispatchWorkflowRequestDto requestDto) {
        super(aggregateId);
        this.requestDto = requestDto;
    }

    @Override
    public String getCommandType() {
        return "IncidentDispatchWorkflowCommand";
    }

    public IncidentDispatchWorkflowRequestDto getRequestDto() {
        return requestDto;
    }

    public void setRequestDto(IncidentDispatchWorkflowRequestDto requestDto) {
        this.requestDto = requestDto;
    }
}
