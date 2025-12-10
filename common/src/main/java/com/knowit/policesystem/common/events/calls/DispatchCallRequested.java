package com.knowit.policesystem.common.events.calls;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to dispatch a call.
 */
public class DispatchCallRequested extends Event {

    private String callId;
    private Instant dispatchedTime;

    public DispatchCallRequested() {
        super();
    }

    public DispatchCallRequested(String callId, Instant dispatchedTime) {
        super(callId);
        this.callId = callId;
        this.dispatchedTime = dispatchedTime;
    }

    @Override
    public String getEventType() {
        return "DispatchCallRequested";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public Instant getDispatchedTime() {
        return dispatchedTime;
    }

    public void setDispatchedTime(Instant dispatchedTime) {
        this.dispatchedTime = dispatchedTime;
    }
}
