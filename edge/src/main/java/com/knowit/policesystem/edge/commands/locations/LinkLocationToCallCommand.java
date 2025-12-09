package com.knowit.policesystem.edge.commands.locations;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.LocationRoleType;
import com.knowit.policesystem.edge.dto.LinkLocationRequestDto;

/**
 * Command for linking a location to a call.
 * This command is processed by LinkLocationToCallCommandHandler.
 */
public class LinkLocationToCallCommand extends Command {

    private String callId;
    private String locationId;
    private LocationRoleType locationRoleType;
    private String description;

    /**
     * Default constructor for deserialization.
     */
    public LinkLocationToCallCommand() {
        super();
    }

    /**
     * Creates a new link location to call command from a DTO.
     *
     * @param aggregateId the aggregate identifier (locationId)
     * @param callId the call ID from the path
     * @param dto the request DTO containing location link data
     */
    public LinkLocationToCallCommand(String aggregateId, String callId, LinkLocationRequestDto dto) {
        super(aggregateId);
        this.callId = callId;
        this.locationId = dto.getLocationId();
        this.locationRoleType = dto.getLocationRoleType();
        this.description = dto.getDescription();
    }

    @Override
    public String getCommandType() {
        return "LinkLocationToCallCommand";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
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
