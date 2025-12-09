package com.knowit.policesystem.edge.commands.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.DispatchIncidentRequestDto;

import java.time.Instant;

/**
 * Command for dispatching an incident.
 * Processed by DispatchIncidentCommandHandler.
 */
public class DispatchIncidentCommand extends Command {

    private String incidentId;
    private Instant dispatchedTime;

    /**
     * Default constructor for deserialization.
     */
    public DispatchIncidentCommand() {
        super();
    }

    /**
     * Creates a new dispatch incident command from a DTO.
     *
     * @param aggregateId the aggregate identifier (incidentId)
     * @param dto the dispatch request DTO
     */
    public DispatchIncidentCommand(String aggregateId, DispatchIncidentRequestDto dto) {
        super(aggregateId);
        this.incidentId = aggregateId;
        this.dispatchedTime = dto.getDispatchedTime();
    }

    @Override
    public String getCommandType() {
        return "DispatchIncidentCommand";
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public Instant getDispatchedTime() {
        return dispatchedTime;
    }

    public void setDispatchedTime(Instant dispatchedTime) {
        this.dispatchedTime = dispatchedTime;
    }
}
