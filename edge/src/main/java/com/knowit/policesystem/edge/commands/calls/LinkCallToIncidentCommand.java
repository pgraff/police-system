package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.LinkCallToIncidentRequestDto;

/**
 * Command for linking a call to an incident.
 */
public class LinkCallToIncidentCommand extends Command {

    private String callId;
    private String incidentId;

    /**
     * Default constructor for deserialization.
     */
    public LinkCallToIncidentCommand() {
        super();
    }

    /**
     * Creates a new link call to incident command.
     *
     * @param aggregateId the aggregate identifier (callId)
     * @param callId the call ID from the path
     * @param dto the request DTO containing incident link data
     */
    public LinkCallToIncidentCommand(String aggregateId, String callId, LinkCallToIncidentRequestDto dto) {
        super(aggregateId);
        this.callId = callId;
        this.incidentId = dto.getIncidentId();
    }

    @Override
    public String getCommandType() {
        return "LinkCallToIncidentCommand";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }
}
