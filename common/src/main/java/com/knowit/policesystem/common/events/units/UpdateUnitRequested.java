package com.knowit.policesystem.common.events.units;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to update a unit.
 * This event is published to Kafka and NATS/JetStream when a unit is updated via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 * All fields are nullable to support partial updates - null fields mean "don't update this field".
 */
public class UpdateUnitRequested extends Event {

    private String unitId;
    private String unitType;
    private String status;

    /**
     * Default constructor for deserialization.
     */
    public UpdateUnitRequested() {
        super();
    }

    /**
     * Creates a new UpdateUnitRequested event.
     *
     * @param unitId the unit ID (used as aggregateId)
     * @param unitType the unit type as string (nullable for partial updates)
     * @param status the status as string (nullable for partial updates)
     */
    public UpdateUnitRequested(String unitId, String unitType, String status) {
        super(unitId);
        this.unitId = unitId;
        this.unitType = unitType;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "UpdateUnitRequested";
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
