package com.knowit.policesystem.common.events.locations;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to unlink a location from an incident.
 * This event is published to Kafka when a location is unlinked from an incident via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class UnlinkLocationFromIncidentRequested extends Event {

    private String incidentId;
    private String locationId;

    /**
     * Default constructor for deserialization.
     */
    public UnlinkLocationFromIncidentRequested() {
        super();
    }

    /**
     * Creates a new UnlinkLocationFromIncidentRequested event.
     *
     * @param aggregateId the aggregate identifier (locationId)
     * @param incidentId the incident ID
     * @param locationId the location ID
     */
    public UnlinkLocationFromIncidentRequested(String aggregateId, String incidentId, String locationId) {
        super(aggregateId);
        this.incidentId = incidentId;
        this.locationId = locationId;
    }

    @Override
    public String getEventType() {
        return "UnlinkLocationFromIncidentRequested";
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
