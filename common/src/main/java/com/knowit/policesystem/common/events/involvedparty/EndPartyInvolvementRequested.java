package com.knowit.policesystem.common.events.involvedparty;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to end a party involvement.
 * Published to Kafka when a party involvement end is requested via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class EndPartyInvolvementRequested extends Event {

    private String involvementId;
    private Instant involvementEndTime;

    /**
     * Default constructor for deserialization.
     */
    public EndPartyInvolvementRequested() {
        super();
    }

    /**
     * Creates a new EndPartyInvolvementRequested event.
     *
     * @param aggregateId the aggregate identifier (involvementId)
     * @param involvementId the involvement identifier
     * @param involvementEndTime the involvement end time
     */
    public EndPartyInvolvementRequested(String aggregateId, String involvementId, Instant involvementEndTime) {
        super(aggregateId);
        this.involvementId = involvementId;
        this.involvementEndTime = involvementEndTime;
    }

    @Override
    public String getEventType() {
        return "EndPartyInvolvementRequested";
    }

    public String getInvolvementId() {
        return involvementId;
    }

    public void setInvolvementId(String involvementId) {
        this.involvementId = involvementId;
    }

    public Instant getInvolvementEndTime() {
        return involvementEndTime;
    }

    public void setInvolvementEndTime(Instant involvementEndTime) {
        this.involvementEndTime = involvementEndTime;
    }
}
