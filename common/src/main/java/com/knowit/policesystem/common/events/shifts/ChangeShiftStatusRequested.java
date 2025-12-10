package com.knowit.policesystem.common.events.shifts;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to change a shift's status.
 * This event is published to Kafka when a shift status is changed via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class ChangeShiftStatusRequested extends Event {

    private String shiftId;
    private String status;

    /**
     * Default constructor for deserialization.
     */
    public ChangeShiftStatusRequested() {
        super();
    }

    /**
     * Creates a new ChangeShiftStatusRequested event.
     *
     * @param shiftId the shift identifier (used as aggregateId)
     * @param status the status as string
     */
    public ChangeShiftStatusRequested(String shiftId, String status) {
        super(shiftId);
        this.shiftId = shiftId;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "ChangeShiftStatusRequested";
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
