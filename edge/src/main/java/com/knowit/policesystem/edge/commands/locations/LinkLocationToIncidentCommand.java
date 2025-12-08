package com.knowit.policesystem.edge.commands.locations;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.LocationRoleType;
import com.knowit.policesystem.edge.dto.LinkLocationRequestDto;

/**
 * Command for linking a location to an incident.
 * This command is processed by LinkLocationToIncidentCommandHandler.
 */
public class LinkLocationToIncidentCommand extends Command {

    private String incidentId;
    private String locationId;
    private LocationRoleType locationRoleType;
    private String description;

    /**
     * Default constructor for deserialization.
     */
    public LinkLocationToIncidentCommand() {
        super();
    }

    /**
     * Creates a new link location to incident command from a DTO.
     *
     * @param aggregateId the aggregate identifier (locationId)
     * @param incidentId the incident ID from the path
     * @param dto the request DTO containing location link data
     */
    public LinkLocationToIncidentCommand(String aggregateId, String incidentId, LinkLocationRequestDto dto) {
        super(aggregateId);
        this.incidentId = incidentId;
        this.locationId = dto.getLocationId();
        this.locationRoleType = dto.getLocationRoleType();
        this.description = dto.getDescription();
    }

    @Override
    public String getCommandType() {
        return "LinkLocationToIncidentCommand";
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

    public LocationRoleType getLocationRoleType() {
        return locationRoleType;
    }

    public void setLocationRoleType(LocationRoleType locationRoleType) {
        this.locationRoleType = locationRoleType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
