package com.knowit.policesystem.common.events.incidents;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to mark an incident as arrived.
 * Published to Kafka (and NATS/JetStream via DualEventPublisher for critical events).
 */
public class ArriveAtIncidentRequested extends Event {

    private String incidentId;
    private Instant arrivedTime;

    /** Default constructor for deserialization. */
    public ArriveAtIncidentRequested() {
        super();
    }

    /**
     * Creates a new ArriveAtIncidentRequested event.
     *
     * @param incidentId the incident identifier (aggregateId)
     * @param arrivedTime the time the incident was arrived
     */
    public ArriveAtIncidentRequested(String incidentId, Instant arrivedTime) {
        super(incidentId);
        this.incidentId = incidentId;
        this.arrivedTime = arrivedTime;
    }

    @Override
    public String getEventType() {
        return "ArriveAtIncidentRequested";
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public Instant getArrivedTime() {
        return arrivedTime;
    }

    public void setArrivedTime(Instant arrivedTime) {
        this.arrivedTime = arrivedTime;
    }
}
