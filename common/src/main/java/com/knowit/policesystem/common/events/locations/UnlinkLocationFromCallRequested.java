package com.knowit.policesystem.common.events.locations;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to unlink a location from a call.
 * Published to Kafka when the unlink REST endpoint is invoked.
 */
public class UnlinkLocationFromCallRequested extends Event {

    private String callId;
    private String locationId;

    /** Default constructor for deserialization. */
    public UnlinkLocationFromCallRequested() {
        super();
    }

    /**
     * Creates a new UnlinkLocationFromCallRequested event.
     *
     * @param aggregateId the aggregate identifier (locationId)
     * @param callId the call ID
     * @param locationId the location ID
     */
    public UnlinkLocationFromCallRequested(String aggregateId, String callId, String locationId) {
        super(aggregateId);
        this.callId = callId;
        this.locationId = locationId;
    }

    @Override
    public String getEventType() {
        return "UnlinkLocationFromCallRequested";
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
