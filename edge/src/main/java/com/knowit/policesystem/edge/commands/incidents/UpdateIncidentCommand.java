package com.knowit.policesystem.edge.commands.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.IncidentType;
import com.knowit.policesystem.edge.domain.Priority;
import com.knowit.policesystem.edge.dto.UpdateIncidentRequestDto;

/**
 * Command for updating an incident.
 * Processed by UpdateIncidentCommandHandler.
 */
public class UpdateIncidentCommand extends Command {

    private String incidentId;
    private Priority priority;
    private String description;
    private IncidentType incidentType;

    /** Default constructor for deserialization. */
    public UpdateIncidentCommand() {
        super();
    }

    /**
     * Creates a new update-incident command from a DTO.
     *
     * @param aggregateId the aggregate identifier (incidentId)
     * @param dto the update incident request DTO
     */
    public UpdateIncidentCommand(String aggregateId, UpdateIncidentRequestDto dto) {
        super(aggregateId);
        this.incidentId = aggregateId;
        this.priority = dto.getPriority();
        this.description = dto.getDescription();
        this.incidentType = dto.getIncidentType();
    }

    @Override
    public String getCommandType() {
        return "UpdateIncidentCommand";
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IncidentType getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(IncidentType incidentType) {
        this.incidentType = incidentType;
    }
}
