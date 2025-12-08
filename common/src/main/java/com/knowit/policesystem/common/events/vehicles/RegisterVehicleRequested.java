package com.knowit.policesystem.common.events.vehicles;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to register a vehicle.
 * This event is published to Kafka and NATS/JetStream when a vehicle is registered via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class RegisterVehicleRequested extends Event {

    private String unitId;
    private String vehicleType;
    private String licensePlate;
    private String vin;
    private String status;
    private String lastMaintenanceDate;

    /**
     * Default constructor for deserialization.
     */
    public RegisterVehicleRequested() {
        super();
    }

    /**
     * Creates a new RegisterVehicleRequested event.
     *
     * @param unitId the unit ID (used as aggregateId)
     * @param vehicleType the vehicle type as string
     * @param licensePlate the license plate
     * @param vin the VIN
     * @param status the status as string
     * @param lastMaintenanceDate the last maintenance date as string (ISO-8601 format: yyyy-MM-dd)
     */
    public RegisterVehicleRequested(String unitId, String vehicleType, String licensePlate,
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
        return "RegisterVehicleRequested";
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
