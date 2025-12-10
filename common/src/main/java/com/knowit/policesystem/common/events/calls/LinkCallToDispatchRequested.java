package com.knowit.policesystem.common.events.calls;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to link a call to a dispatch.
 * This event is published to Kafka when a call is linked to a dispatch via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class LinkCallToDispatchRequested extends Event {

    private String callId;
    private String dispatchId;

    /**
     * Default constructor for deserialization.
     */
    public LinkCallToDispatchRequested() {
        super();
    }

    /**
     * Creates a new LinkCallToDispatchRequested event.
     *
     * @param aggregateId the aggregate identifier (callId)
     * @param callId the call ID
     * @param dispatchId the dispatch ID
     */
    public LinkCallToDispatchRequested(String aggregateId, String callId, String dispatchId) {
        super(aggregateId);
        this.callId = callId;
        this.dispatchId = dispatchId;
    }

    @Override
    public String getEventType() {
        return "LinkCallToDispatchRequested";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }
}
