package com.knowit.policesystem.common.events.activities;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to link an activity to an incident.
 * This event is published to Kafka when an activity is linked to an incident via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class LinkActivityToIncidentRequested extends Event {

    private String activityId;
    private String incidentId;

    /**
     * Default constructor for deserialization.
     */
    public LinkActivityToIncidentRequested() {
        super();
    }

    /**
     * Creates a new LinkActivityToIncidentRequested event.
     *
     * @param aggregateId the aggregate identifier (activityId)
     * @param activityId the activity ID
     * @param incidentId the incident ID
     */
    public LinkActivityToIncidentRequested(String aggregateId, String activityId, String incidentId) {
        super(aggregateId);
        this.activityId = activityId;
        this.incidentId = incidentId;
    }

    @Override
    public String getEventType() {
        return "LinkActivityToIncidentRequested";
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
