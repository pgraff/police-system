package com.knowit.policesystem.edge.commands.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.ClearIncidentRequestDto;

import java.time.Instant;

/**
 * Command for clearing an incident.
 * Processed by ClearIncidentCommandHandler.
 */
public class ClearIncidentCommand extends Command {

    private String incidentId;
    private Instant clearedTime;

    /** Default constructor for deserialization. */
    public ClearIncidentCommand() {
        super();
    }

    /**
     * Creates a new clear-incident command from a DTO.
     *
     * @param aggregateId the aggregate identifier (incidentId)
     * @param dto the clear request DTO
     */
    public ClearIncidentCommand(String aggregateId, ClearIncidentRequestDto dto) {
        super(aggregateId);
        this.incidentId = aggregateId;
        this.clearedTime = dto.getClearedTime();
    }

    @Override
    public String getCommandType() {
        return "ClearIncidentCommand";
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public Instant getClearedTime() {
        return clearedTime;
    }

    public void setClearedTime(Instant clearedTime) {
        this.clearedTime = clearedTime;
    }
}
