package com.knowit.policesystem.edge.commands.locations;

import com.knowit.policesystem.edge.commands.Command;

/**
 * Command for unlinking a location from a call.
 * Processed by UnlinkLocationFromCallCommandHandler.
 */
public class UnlinkLocationFromCallCommand extends Command {

    private String callId;
    private String locationId;

    /** Default constructor for deserialization. */
    public UnlinkLocationFromCallCommand() {
        super();
    }

    /**
     * Creates a new unlink location from call command.
     *
     * @param aggregateId the aggregate identifier (locationId)
     * @param callId the call ID from the path
     * @param locationId the location ID from the path
     */
    public UnlinkLocationFromCallCommand(String aggregateId, String callId, String locationId) {
        super(aggregateId);
        this.callId = callId;
        this.locationId = locationId;
    }

    @Override
    public String getCommandType() {
        return "UnlinkLocationFromCallCommand";
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
}
