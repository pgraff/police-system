package com.knowit.policesystem.common.events.dispatches;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to change a dispatch's status.
 * This event is published to Kafka when a dispatch status is changed via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class ChangeDispatchStatusRequested extends Event {

    private String dispatchId;
    private String status;

    /**
     * Default constructor for deserialization.
     */
    public ChangeDispatchStatusRequested() {
        super();
    }

    /**
     * Creates a new ChangeDispatchStatusRequested event.
     *
     * @param dispatchId the dispatch identifier (used as aggregateId)
     * @param status the status as string
     */
    public ChangeDispatchStatusRequested(String dispatchId, String status) {
        super(dispatchId);
        this.dispatchId = dispatchId;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "ChangeDispatchStatusRequested";
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
