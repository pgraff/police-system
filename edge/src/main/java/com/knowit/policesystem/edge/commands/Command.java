package com.knowit.policesystem.edge.commands;

import java.time.Instant;
import java.util.UUID;

/**
 * Base abstract class for all commands in the system.
 * Provides common metadata fields: commandId, timestamp, and aggregateId.
 * All domain commands must extend this class.
 */
public abstract class Command {
    private UUID commandId;
    private Instant timestamp;
    private String aggregateId;

    /**
     * Default constructor that generates commandId and sets timestamp.
     */
    protected Command() {
        this.commandId = UUID.randomUUID();
        this.timestamp = Instant.now();
    }

    /**
     * Constructor with aggregateId.
     *
     * @param aggregateId the aggregate identifier this command targets
     */
    protected Command(String aggregateId) {
        this();
        this.aggregateId = aggregateId;
    }

    /**
     * Full constructor for deserialization.
     *
     * @param commandId the unique command identifier
     * @param timestamp the command timestamp
     * @param aggregateId the aggregate identifier
     */
    protected Command(UUID commandId, Instant timestamp, String aggregateId) {
        this.commandId = commandId;
        this.timestamp = timestamp;
        this.aggregateId = aggregateId;
    }

    /**
     * Returns the unique identifier for this command.
     *
     * @return the command ID
     */
    public UUID getCommandId() {
        return commandId;
    }

    /**
     * Sets the command ID (used for deserialization).
     *
     * @param commandId the command ID
     */
    public void setCommandId(UUID commandId) {
        this.commandId = commandId;
    }

    /**
     * Returns the timestamp when this command was created.
     *
     * @return the command timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp (used for deserialization).
     *
     * @param timestamp the command timestamp
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the aggregate identifier this command targets.
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
     * Returns the command type identifier.
     * This is used for command type identification and routing.
     *
     * @return the command type name
     */
    public abstract String getCommandType();
}

