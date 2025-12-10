package com.knowit.policesystem.common.events.shifts;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to end a shift.
 * Published to Kafka when a shift end is requested via the REST API.
 */
public class EndShiftRequested extends Event {

    private String shiftId;
    private Instant endTime;

    /**
     * Default constructor for deserialization.
     */
    public EndShiftRequested() {
        super();
    }

    /**
     * Creates a new EndShiftRequested event.
     *
     * @param shiftId the shift identifier (aggregateId)
     * @param endTime the end time of the shift
     */
    public EndShiftRequested(String shiftId, Instant endTime) {
        super(shiftId);
        this.shiftId = shiftId;
        this.endTime = endTime;
    }

    @Override
    public String getEventType() {
        return "EndShiftRequested";
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
}
