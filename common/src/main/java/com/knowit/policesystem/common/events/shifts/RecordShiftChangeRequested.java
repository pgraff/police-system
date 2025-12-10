package com.knowit.policesystem.common.events.shifts;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to record a shift change.
 * This event is published to Kafka when a shift change is recorded via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class RecordShiftChangeRequested extends Event {

    private String shiftId;
    private String shiftChangeId;
    private Instant changeTime;
    private String changeType;
    private String notes;

    /**
     * Default constructor for deserialization.
     */
    public RecordShiftChangeRequested() {
        super();
    }

    /**
     * Creates a new RecordShiftChangeRequested event.
     *
     * @param shiftId the shift identifier (used as aggregateId)
     * @param shiftChangeId the shift change identifier
     * @param changeTime the change time
     * @param changeType the change type as string
     * @param notes optional notes
     */
    public RecordShiftChangeRequested(String shiftId, String shiftChangeId, Instant changeTime, String changeType, String notes) {
        super(shiftId);
        this.shiftId = shiftId;
        this.shiftChangeId = shiftChangeId;
        this.changeTime = changeTime;
        this.changeType = changeType;
        this.notes = notes;
    }

    @Override
    public String getEventType() {
        return "RecordShiftChangeRequested";
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public String getShiftChangeId() {
        return shiftChangeId;
    }

    public void setShiftChangeId(String shiftChangeId) {
        this.shiftChangeId = shiftChangeId;
    }

    public Instant getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(Instant changeTime) {
        this.changeTime = changeTime;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
