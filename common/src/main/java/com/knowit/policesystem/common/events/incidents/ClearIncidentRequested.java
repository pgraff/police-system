package com.knowit.policesystem.common.events.incidents;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to clear an incident.
 * Published to Kafka (and NATS/JetStream via DualEventPublisher for critical events).
 */
public class ClearIncidentRequested extends Event {

    private String incidentId;
    private Instant clearedTime;

    /** Default constructor for deserialization. */
    public ClearIncidentRequested() {
        super();
    }

    /**
     * Creates a new ClearIncidentRequested event.
     *
     * @param incidentId the incident identifier (aggregateId)
     * @param clearedTime the time the incident was cleared
     */
    public ClearIncidentRequested(String incidentId, Instant clearedTime) {
        super(incidentId);
        this.incidentId = incidentId;
        this.clearedTime = clearedTime;
    }

    @Override
    public String getEventType() {
        return "ClearIncidentRequested";
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public Instant getClearedTime() {
        return clearedTime;
    }

    public void setClearedTime(Instant clearedTime) {
        this.clearedTime = clearedTime;
    }
}
