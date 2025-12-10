package com.knowit.policesystem.common.events.officershifts;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to check out an officer from a shift.
 * Published to Kafka when an officer check-out is requested via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class CheckOutOfficerRequested extends Event {

    private String shiftId;
    private String badgeNumber;
    private Instant checkOutTime;

    /**
     * Default constructor for deserialization.
     */
    public CheckOutOfficerRequested() {
        super();
    }

    /**
     * Creates a new CheckOutOfficerRequested event.
     *
     * @param aggregateId the aggregate identifier (shiftId)
     * @param shiftId the shift identifier
     * @param badgeNumber the badge number
     * @param checkOutTime the check-out time
     */
    public CheckOutOfficerRequested(String aggregateId, String shiftId, String badgeNumber, Instant checkOutTime) {
        super(aggregateId);
        this.shiftId = shiftId;
        this.badgeNumber = badgeNumber;
        this.checkOutTime = checkOutTime;
    }

    @Override
    public String getEventType() {
        return "CheckOutOfficerRequested";
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public Instant getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(Instant checkOutTime) {
        this.checkOutTime = checkOutTime;
    }
}
