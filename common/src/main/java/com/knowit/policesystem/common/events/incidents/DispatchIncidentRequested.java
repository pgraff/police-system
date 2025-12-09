package com.knowit.policesystem.common.events.incidents;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to dispatch an incident.
 * Published to Kafka (and NATS/JetStream via DualEventPublisher for critical events).
 */
public class DispatchIncidentRequested extends Event {

    private String incidentId;
    private Instant dispatchedTime;

    /**
     * Default constructor for deserialization.
     */
    public DispatchIncidentRequested() {
        super();
    }

    /**
     * Creates a new DispatchIncidentRequested event.
     *
     * @param incidentId the incident identifier (aggregateId)
     * @param dispatchedTime the dispatched time
     */
    public DispatchIncidentRequested(String incidentId, Instant dispatchedTime) {
        super(incidentId);
        this.incidentId = incidentId;
        this.dispatchedTime = dispatchedTime;
    }

    @Override
    public String getEventType() {
        return "DispatchIncidentRequested";
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public Instant getDispatchedTime() {
        return dispatchedTime;
    }

    public void setDispatchedTime(Instant dispatchedTime) {
        this.dispatchedTime = dispatchedTime;
    }
}
