package com.knowit.policesystem.common.events.calls;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing an update request for a call.
 */
public class UpdateCallRequested extends Event {

    private String callId;
    private String priority;
    private String description;
    private String callType;

    public UpdateCallRequested() {
        super();
    }

    public UpdateCallRequested(String callId, String priority, String description, String callType) {
        super(callId);
        this.callId = callId;
        this.priority = priority;
        this.description = description;
        this.callType = callType;
    }

    @Override
    public String getEventType() {
        return "UpdateCallRequested";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
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
