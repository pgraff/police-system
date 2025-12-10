package com.knowit.policesystem.common.events.calls;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request for a unit to arrive at a call.
 */
public class ArriveAtCallRequested extends Event {

    private String callId;
    private Instant arrivedTime;

    public ArriveAtCallRequested() {
        super();
    }

    public ArriveAtCallRequested(String callId, Instant arrivedTime) {
        super(callId);
        this.callId = callId;
        this.arrivedTime = arrivedTime;
    }

    @Override
    public String getEventType() {
        return "ArriveAtCallRequested";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public Instant getArrivedTime() {
        return arrivedTime;
    }

    public void setArrivedTime(Instant arrivedTime) {
        this.arrivedTime = arrivedTime;
    }
}
