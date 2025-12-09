package com.knowit.policesystem.common.events.calls;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to receive a call.
 * This event is published to Kafka when a call is received via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class ReceiveCallRequested extends Event {

    private String callId;
    private String callNumber;
    private String priority;
    private String status;
    private Instant receivedTime;
    private String description;
    private String callType;

    /**
     * Default constructor for deserialization.
     */
    public ReceiveCallRequested() {
        super();
    }

    /**
     * Creates a new ReceiveCallRequested event.
     *
     * @param callId the call identifier (used as aggregateId)
     * @param callNumber the call number
     * @param priority the priority as string
     * @param status the status as string
     * @param receivedTime the received time
     * @param description the description
     * @param callType the call type as string
     */
    public ReceiveCallRequested(String callId, String callNumber, String priority,
                               String status, Instant receivedTime, String description,
                               String callType) {
        super(callId);
        this.callId = callId;
        this.callNumber = callNumber;
        this.priority = priority;
        this.status = status;
        this.receivedTime = receivedTime;
        this.description = description;
        this.callType = callType;
    }

    @Override
    public String getEventType() {
        return "ReceiveCallRequested";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getCallNumber() {
        return callNumber;
    }

    public void setCallNumber(String callNumber) {
        this.callNumber = callNumber;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(Instant receivedTime) {
        this.receivedTime = receivedTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }
}
