package com.knowit.policesystem.common.events.units;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to change a unit's status.
 * This event is published to Kafka and NATS/JetStream when a unit's status is changed via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class ChangeUnitStatusRequested extends Event {

    private String unitId;
    private String status;

    /**
     * Default constructor for deserialization.
     */
    public ChangeUnitStatusRequested() {
        super();
    }

    /**
     * Creates a new ChangeUnitStatusRequested event.
     *
     * @param unitId the unit ID (used as aggregateId)
     * @param status the new status
     */
    public ChangeUnitStatusRequested(String unitId, String status) {
        super(unitId);
        this.unitId = unitId;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "ChangeUnitStatusRequested";
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
