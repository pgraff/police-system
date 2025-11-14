package com.knowit.policesystem.edge.queries;

import java.time.Instant;
import java.util.UUID;

/**
 * Base abstract class for all queries in the system.
 * Provides common metadata fields: queryId and timestamp.
 * All domain queries must extend this class.
 */
public abstract class Query {
    private UUID queryId;
    private Instant timestamp;

    /**
     * Default constructor that generates queryId and sets timestamp.
     */
    protected Query() {
        this.queryId = UUID.randomUUID();
        this.timestamp = Instant.now();
    }

    /**
     * Full constructor for deserialization.
     *
     * @param queryId the unique query identifier
     * @param timestamp the query timestamp
     */
    protected Query(UUID queryId, Instant timestamp) {
        this.queryId = queryId;
        this.timestamp = timestamp;
    }

    /**
     * Returns the unique identifier for this query.
     *
     * @return the query ID
     */
    public UUID getQueryId() {
        return queryId;
    }

    /**
     * Sets the query ID (used for deserialization).
     *
     * @param queryId the query ID
     */
    public void setQueryId(UUID queryId) {
        this.queryId = queryId;
    }

    /**
     * Returns the timestamp when this query was created.
     *
     * @return the query timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp (used for deserialization).
     *
     * @param timestamp the query timestamp
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the query type identifier.
     * This is used for query type identification and routing.
     *
     * @return the query type name
     */
    public abstract String getQueryType();
}

