package com.knowit.policesystem.edge.commands.locations;

import com.knowit.policesystem.edge.commands.Command;

/**
 * Command for unlinking a location from an incident.
 * This command is processed by UnlinkLocationFromIncidentCommandHandler.
 */
public class UnlinkLocationFromIncidentCommand extends Command {

    private String incidentId;
    private String locationId;

    /**
     * Default constructor for deserialization.
     */
    public UnlinkLocationFromIncidentCommand() {
        super();
    }

    /**
     * Creates a new unlink location from incident command.
     *
     * @param aggregateId the aggregate identifier (locationId)
     * @param incidentId the incident ID from the path
     * @param locationId the location ID from the path
     */
    public UnlinkLocationFromIncidentCommand(String aggregateId, String incidentId, String locationId) {
        super(aggregateId);
        this.incidentId = incidentId;
        this.locationId = locationId;
    }

    @Override
    public String getCommandType() {
        return "UnlinkLocationFromIncidentCommand";
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }
}
