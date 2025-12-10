package com.knowit.policesystem.edge.commands.activities;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.LinkActivityToIncidentRequestDto;

/**
 * Command for linking an activity to an incident.
 */
public class LinkActivityToIncidentCommand extends Command {

    private String activityId;
    private String incidentId;

    /**
     * Default constructor for deserialization.
     */
    public LinkActivityToIncidentCommand() {
        super();
    }

    /**
     * Creates a new link activity to incident command.
     *
     * @param aggregateId the aggregate identifier (activityId)
     * @param activityId the activity ID from the path
     * @param dto the request DTO containing incident link data
     */
    public LinkActivityToIncidentCommand(String aggregateId, String activityId, LinkActivityToIncidentRequestDto dto) {
        super(aggregateId);
        this.activityId = activityId;
        this.incidentId = dto.getIncidentId();
    }

    @Override
    public String getCommandType() {
        return "LinkActivityToIncidentCommand";
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }
}
