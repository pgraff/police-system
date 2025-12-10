package com.knowit.policesystem.common.events.officershifts;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to update an officer shift.
 * Published to Kafka when an officer shift update is requested via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class UpdateOfficerShiftRequested extends Event {

    private String shiftId;
    private String badgeNumber;
    private String shiftRoleType;

    /**
     * Default constructor for deserialization.
     */
    public UpdateOfficerShiftRequested() {
        super();
    }

    /**
     * Creates a new UpdateOfficerShiftRequested event.
     *
     * @param aggregateId the aggregate identifier (shiftId)
     * @param shiftId the shift identifier
     * @param badgeNumber the badge number
     * @param shiftRoleType the shift role type (as String)
     */
    public UpdateOfficerShiftRequested(String aggregateId, String shiftId, String badgeNumber, String shiftRoleType) {
        super(aggregateId);
        this.shiftId = shiftId;
        this.badgeNumber = badgeNumber;
        this.shiftRoleType = shiftRoleType;
    }

    @Override
    public String getEventType() {
        return "UpdateOfficerShiftRequested";
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

    public String getShiftRoleType() {
        return shiftRoleType;
    }

    public void setShiftRoleType(String shiftRoleType) {
        this.shiftRoleType = shiftRoleType;
    }
}
