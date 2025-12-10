package com.knowit.policesystem.common.events.shifts;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to start a shift.
 * Published to Kafka when a shift start is requested via the REST API.
 */
public class StartShiftRequested extends Event {

    private String shiftId;
    private Instant startTime;
    private Instant endTime;
    private String shiftType;
    private String status;

    /**
     * Default constructor for deserialization.
     */
    public StartShiftRequested() {
        super();
    }

    /**
     * Creates a new StartShiftRequested event.
     *
     * @param shiftId the shift identifier (used as aggregateId)
     * @param startTime the optional shift start time
     * @param endTime the optional shift end time
     * @param shiftType the shift type as string
     * @param status the shift status as string
     */
    public StartShiftRequested(String shiftId, Instant startTime, Instant endTime, String shiftType, String status) {
        super(shiftId);
        this.shiftId = shiftId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.shiftType = shiftType;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "StartShiftRequested";
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
