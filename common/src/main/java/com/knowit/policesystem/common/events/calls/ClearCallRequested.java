package com.knowit.policesystem.common.events.calls;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to clear a call.
 */
public class ClearCallRequested extends Event {

    private String callId;
    private Instant clearedTime;

    public ClearCallRequested() {
        super();
    }

    public ClearCallRequested(String callId, Instant clearedTime) {
        super(callId);
        this.callId = callId;
        this.clearedTime = clearedTime;
    }

    @Override
    public String getEventType() {
        return "ClearCallRequested";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public Instant getClearedTime() {
        return clearedTime;
    }

    public void setClearedTime(Instant clearedTime) {
        this.clearedTime = clearedTime;
    }
}
