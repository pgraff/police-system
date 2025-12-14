package com.knowit.policesystem.edge.commands.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.CreateIncidentWithRelationsRequestDto;

/**
 * Command for creating an incident with related resources (location, calls) in one operation.
 * Processed by CreateIncidentWithRelationsCommandHandler.
 */
public class CreateIncidentWithRelationsCommand extends Command {

    private CreateIncidentWithRelationsRequestDto requestDto;

    /**
     * Default constructor for deserialization.
     */
    public CreateIncidentWithRelationsCommand() {
        super();
    }

    /**
     * Creates a new create incident with relations command.
     *
     * @param aggregateId the aggregate identifier (incidentId)
     * @param requestDto the create incident with relations request DTO
     */
    public CreateIncidentWithRelationsCommand(String aggregateId, CreateIncidentWithRelationsRequestDto requestDto) {
        super(aggregateId);
        this.requestDto = requestDto;
    }

    @Override
    public String getCommandType() {
        return "CreateIncidentWithRelationsCommand";
    }

    public CreateIncidentWithRelationsRequestDto getRequestDto() {
        return requestDto;
    }

    public void setRequestDto(CreateIncidentWithRelationsRequestDto requestDto) {
        this.requestDto = requestDto;
    }
}
