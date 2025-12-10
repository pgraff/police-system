package com.knowit.policesystem.common.events.calls;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to change call status.
 */
public class ChangeCallStatusRequested extends Event {

    private String callId;
    private String status;

    public ChangeCallStatusRequested() {
        super();
    }

    public ChangeCallStatusRequested(String callId, String status) {
        super(callId);
        this.callId = callId;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "ChangeCallStatusRequested";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
