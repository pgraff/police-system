package com.knowit.policesystem.common.events.officershifts;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to check in an officer to a shift.
 * Published to Kafka when an officer check-in is requested via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class CheckInOfficerRequested extends Event {

    private String shiftId;
    private String badgeNumber;
    private Instant checkInTime;
    private String shiftRoleType;

    /**
     * Default constructor for deserialization.
     */
    public CheckInOfficerRequested() {
        super();
    }

    /**
     * Creates a new CheckInOfficerRequested event.
     *
     * @param aggregateId the aggregate identifier (shiftId)
     * @param shiftId the shift identifier
     * @param badgeNumber the badge number
     * @param checkInTime the check-in time
     * @param shiftRoleType the shift role type (as String)
     */
    public CheckInOfficerRequested(String aggregateId, String shiftId, String badgeNumber, Instant checkInTime, String shiftRoleType) {
        super(aggregateId);
        this.shiftId = shiftId;
        this.badgeNumber = badgeNumber;
        this.checkInTime = checkInTime;
        this.shiftRoleType = shiftRoleType;
    }

    @Override
    public String getEventType() {
        return "CheckInOfficerRequested";
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

    public Instant getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(Instant checkInTime) {
        this.checkInTime = checkInTime;
    }

    public String getShiftRoleType() {
        return shiftRoleType;
    }

    public void setShiftRoleType(String shiftRoleType) {
        this.shiftRoleType = shiftRoleType;
    }
}
