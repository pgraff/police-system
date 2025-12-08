package com.knowit.policesystem.common.events.vehicles;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to update a vehicle.
 * This event is published to Kafka and NATS/JetStream when a vehicle is updated via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 * All fields are nullable to support partial updates - null fields mean "don't update this field".
 */
public class UpdateVehicleRequested extends Event {

    private String unitId;
    private String vehicleType;
    private String licensePlate;
    private String vin;
    private String status;
    private String lastMaintenanceDate;

    /**
     * Default constructor for deserialization.
     */
    public UpdateVehicleRequested() {
        super();
    }

    /**
     * Creates a new UpdateVehicleRequested event.
     *
     * @param unitId the unit ID (used as aggregateId)
     * @param vehicleType the vehicle type as string (nullable for partial updates)
     * @param licensePlate the license plate (nullable for partial updates)
     * @param vin the VIN (nullable for partial updates)
     * @param status the status as string (nullable for partial updates)
     * @param lastMaintenanceDate the last maintenance date as string (ISO-8601 format: yyyy-MM-dd, nullable for partial updates)
     */
    public UpdateVehicleRequested(String unitId, String vehicleType, String licensePlate,
                                 String vin, String status, String lastMaintenanceDate) {
        super(unitId);
        this.unitId = unitId;
        this.vehicleType = vehicleType;
        this.licensePlate = licensePlate;
        this.vin = vin;
        this.status = status;
        this.lastMaintenanceDate = lastMaintenanceDate;
    }

    @Override
    public String getEventType() {
        return "UpdateVehicleRequested";
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastMaintenanceDate() {
        return lastMaintenanceDate;
    }

    public void setLastMaintenanceDate(String lastMaintenanceDate) {
        this.lastMaintenanceDate = lastMaintenanceDate;
    }
}
