package com.knowit.policesystem.edge.commands.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.ArriveAtIncidentRequestDto;

import java.time.Instant;

/**
 * Command for marking an incident as arrived.
 * Processed by ArriveAtIncidentCommandHandler.
 */
public class ArriveAtIncidentCommand extends Command {

    private String incidentId;
    private Instant arrivedTime;

    /** Default constructor for deserialization. */
    public ArriveAtIncidentCommand() {
        super();
    }

    /**
     * Creates a new arrive-at-incident command from a DTO.
     *
     * @param aggregateId the aggregate identifier (incidentId)
     * @param dto the arrive request DTO
     */
    public ArriveAtIncidentCommand(String aggregateId, ArriveAtIncidentRequestDto dto) {
        super(aggregateId);
        this.incidentId = aggregateId;
        this.arrivedTime = dto.getArrivedTime();
    }

    @Override
    public String getCommandType() {
        return "ArriveAtIncidentCommand";
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public Instant getArrivedTime() {
        return arrivedTime;
    }

    public void setArrivedTime(Instant arrivedTime) {
        this.arrivedTime = arrivedTime;
    }
}
