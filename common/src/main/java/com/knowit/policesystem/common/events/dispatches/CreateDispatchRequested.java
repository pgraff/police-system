package com.knowit.policesystem.common.events.dispatches;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to create a dispatch.
 * This event is published to Kafka when a dispatch is created via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class CreateDispatchRequested extends Event {

    private String dispatchId;
    private Instant dispatchTime;
    private String dispatchType;
    private String status;

    /**
     * Default constructor for deserialization.
     */
    public CreateDispatchRequested() {
        super();
    }

    /**
     * Creates a new CreateDispatchRequested event.
     *
     * @param dispatchId the dispatch ID (used as aggregateId)
     * @param dispatchTime the dispatch time
     * @param dispatchType the dispatch type as string
     * @param status the status as string
     */
    public CreateDispatchRequested(String dispatchId, Instant dispatchTime, String dispatchType, String status) {
        super(dispatchId);
        this.dispatchId = dispatchId;
        this.dispatchTime = dispatchTime;
        this.dispatchType = dispatchType;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "CreateDispatchRequested";
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }

    public Instant getDispatchTime() {
        return dispatchTime;
    }

    public void setDispatchTime(Instant dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    public String getDispatchType() {
        return dispatchType;
    }

    public void setDispatchType(String dispatchType) {
        this.dispatchType = dispatchType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
