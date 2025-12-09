package com.knowit.policesystem.common.events.locations;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to link a location to a call.
 * This event is published to Kafka when a location is linked to a call via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class LinkLocationToCallRequested extends Event {

    private String callId;
    private String locationId;
    private String locationRoleType;
    private String description;

    /**
     * Default constructor for deserialization.
     */
    public LinkLocationToCallRequested() {
        super();
    }

    /**
     * Creates a new LinkLocationToCallRequested event.
     *
     * @param aggregateId the aggregate identifier (locationId)
     * @param callId the call ID
     * @param locationId the location ID
     * @param locationRoleType the location role type as string enum name
     * @param description the description
     */
    public LinkLocationToCallRequested(String aggregateId, String callId, String locationId,
                                       String locationRoleType, String description) {
        super(aggregateId);
        this.callId = callId;
        this.locationId = locationId;
        this.locationRoleType = locationRoleType;
        this.description = description;
    }

    @Override
    public String getEventType() {
        return "LinkLocationToCallRequested";
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

    public String getLocationRoleType() {
        return locationRoleType;
    }

    public void setLocationRoleType(String locationRoleType) {
        this.locationRoleType = locationRoleType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
