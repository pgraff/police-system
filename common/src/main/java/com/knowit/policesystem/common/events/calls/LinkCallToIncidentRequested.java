package com.knowit.policesystem.common.events.calls;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to link a call to an incident.
 * This event is published to Kafka when a call is linked to an incident via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class LinkCallToIncidentRequested extends Event {

    private String callId;
    private String incidentId;

    /**
     * Default constructor for deserialization.
     */
    public LinkCallToIncidentRequested() {
        super();
    }

    /**
     * Creates a new LinkCallToIncidentRequested event.
     *
     * @param aggregateId the aggregate identifier (callId)
     * @param callId the call ID
     * @param incidentId the incident ID
     */
    public LinkCallToIncidentRequested(String aggregateId, String callId, String incidentId) {
        super(aggregateId);
        this.callId = callId;
        this.incidentId = incidentId;
    }

    @Override
    public String getEventType() {
        return "LinkCallToIncidentRequested";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }
}
