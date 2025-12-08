package com.knowit.policesystem.common.events.locations;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to link a location to an incident.
 * This event is published to Kafka when a location is linked to an incident via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class LinkLocationToIncidentRequested extends Event {

    private String incidentId;
    private String locationId;
    private String locationRoleType;
    private String description;

    /**
     * Default constructor for deserialization.
     */
    public LinkLocationToIncidentRequested() {
        super();
    }

    /**
     * Creates a new LinkLocationToIncidentRequested event.
     *
     * @param aggregateId the aggregate identifier (locationId)
     * @param incidentId the incident ID
     * @param locationId the location ID
     * @param locationRoleType the location role type as string enum name
     * @param description the description
     */
    public LinkLocationToIncidentRequested(String aggregateId, String incidentId, String locationId,
                                           String locationRoleType, String description) {
        super(aggregateId);
        this.incidentId = incidentId;
        this.locationId = locationId;
        this.locationRoleType = locationRoleType;
        this.description = description;
    }

    @Override
    public String getEventType() {
        return "LinkLocationToIncidentRequested";
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
