package com.knowit.policesystem.common.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.UUID;

/**
 * Base abstract class for all events in the system.
 * Provides common metadata fields: eventId, timestamp, aggregateId, and version.
 * All domain events must extend this class.
 */
public abstract class Event {
    private UUID eventId;
    private Instant timestamp;
    private String aggregateId;
    private int version;

    /**
     * Default constructor that generates eventId and sets timestamp.
     * Version defaults to 1.
     */
    protected Event() {
        this.eventId = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.version = 1;
    }

    /**
     * Constructor with aggregateId.
     * Version defaults to 1.
     *
     * @param aggregateId the aggregate identifier this event belongs to
     */
    protected Event(String aggregateId) {
        this();
        this.aggregateId = aggregateId;
    }

    /**
     * Constructor with aggregateId and version.
     *
     * @param aggregateId the aggregate identifier this event belongs to
     * @param version the event version for schema evolution
     */
    protected Event(String aggregateId, int version) {
        this();
        this.aggregateId = aggregateId;
        this.version = version;
    }

    /**
     * Full constructor for deserialization.
     *
     * @param eventId the unique event identifier
     * @param timestamp the event timestamp
     * @param aggregateId the aggregate identifier
     * @param version the event version
     */
    protected Event(UUID eventId, Instant timestamp, String aggregateId, int version) {
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.aggregateId = aggregateId;
        this.version = version;
    }

    /**
     * Returns the unique identifier for this event.
     *
     * @return the event ID
     */
    public UUID getEventId() {
        return eventId;
    }

    /**
     * Sets the event ID (used for deserialization).
     *
     * @param eventId the event ID
     */
    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    /**
     * Returns the timestamp when this event was created.
     *
     * @return the event timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp (used for deserialization).
     *
     * @param timestamp the event timestamp
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the aggregate identifier this event belongs to.
     *
     * @return the aggregate ID
     */
    public String getAggregateId() {
        return aggregateId;
    }

    /**
     * Sets the aggregate ID.
     *
     * @param aggregateId the aggregate ID
     */
    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    /**
     * Returns the event version for schema evolution support.
     *
     * @return the event version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Sets the event version.
     *
     * @param version the event version
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Returns the event type identifier.
     * This is used for event type identification and routing.
     *
     * @return the event type name
     */
    @JsonIgnore
    public abstract String getEventType();
}

