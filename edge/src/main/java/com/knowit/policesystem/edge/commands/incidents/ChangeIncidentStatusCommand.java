package com.knowit.policesystem.edge.commands.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.IncidentStatus;
import com.knowit.policesystem.edge.dto.ChangeIncidentStatusRequestDto;

/**
 * Command for changing an incident's status.
 * Processed by ChangeIncidentStatusCommandHandler.
 */
public class ChangeIncidentStatusCommand extends Command {

    private String incidentId;
    private IncidentStatus status;

    /** Default constructor for deserialization. */
    public ChangeIncidentStatusCommand() {
        super();
    }

    /**
     * Creates a new change-incident-status command from a DTO.
     *
     * @param aggregateId the aggregate identifier (incidentId)
     * @param dto the status change request DTO
     */
    public ChangeIncidentStatusCommand(String aggregateId, ChangeIncidentStatusRequestDto dto) {
        super(aggregateId);
        this.incidentId = aggregateId;
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "ChangeIncidentStatusCommand";
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }
}
