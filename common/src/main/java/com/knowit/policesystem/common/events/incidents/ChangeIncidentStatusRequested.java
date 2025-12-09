package com.knowit.policesystem.common.events.incidents;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to change an incident's status.
 * Published via DualEventPublisher to Kafka (and NATS/JetStream for critical events).
 */
public class ChangeIncidentStatusRequested extends Event {

    private String incidentId;
    private String status;

    /** Default constructor for deserialization. */
    public ChangeIncidentStatusRequested() {
        super();
    }

    /**
     * Creates a new ChangeIncidentStatusRequested event.
     *
     * @param incidentId the incident identifier (aggregateId)
     * @param status the new status for the incident
     */
    public ChangeIncidentStatusRequested(String incidentId, String status) {
        super(incidentId);
        this.incidentId = incidentId;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "ChangeIncidentStatusRequested";
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
