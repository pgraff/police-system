package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.knowit.policesystem.edge.domain.VehicleStatus;
import com.knowit.policesystem.edge.domain.VehicleType;

import java.time.LocalDate;

/**
 * Request DTO for updating a vehicle.
 * Matches the UpdateVehicleRequest schema in the OpenAPI specification.
 * All fields are optional for partial updates.
 */
public class UpdateVehicleRequestDto {

    private VehicleType vehicleType;

    private String licensePlate;

    private String vin;

    private VehicleStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate lastMaintenanceDate;

    /**
     * Default constructor for Jackson deserialization.
     */
    public UpdateVehicleRequestDto() {
    }

    /**
     * Creates a new update vehicle request DTO.
     *
     * @param vehicleType the vehicle type (optional)
     * @param licensePlate the license plate (optional)
     * @param vin the VIN (optional)
     * @param status the vehicle status (optional)
     * @param lastMaintenanceDate the last maintenance date (optional)
     */
    public UpdateVehicleRequestDto(VehicleType vehicleType, String licensePlate, String vin,
                                  VehicleStatus status, LocalDate lastMaintenanceDate) {
        this.vehicleType = vehicleType;
        this.licensePlate = licensePlate;
        this.vin = vin;
        this.status = status;
        this.lastMaintenanceDate = lastMaintenanceDate;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
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

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public LocalDate getLastMaintenanceDate() {
        return lastMaintenanceDate;
    }

    public void setLastMaintenanceDate(LocalDate lastMaintenanceDate) {
        this.lastMaintenanceDate = lastMaintenanceDate;
    }
}
