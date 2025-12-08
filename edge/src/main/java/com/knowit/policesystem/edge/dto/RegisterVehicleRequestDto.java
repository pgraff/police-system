package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.knowit.policesystem.edge.domain.VehicleStatus;
import com.knowit.policesystem.edge.domain.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request DTO for registering a vehicle.
 * Matches the RegisterVehicleRequest schema in the OpenAPI specification.
 */
public class RegisterVehicleRequestDto {

    @NotBlank(message = "unitId is required")
    private String unitId;

    @NotNull(message = "vehicleType is required")
    private VehicleType vehicleType;

    @NotBlank(message = "licensePlate is required")
    private String licensePlate;

    @NotBlank(message = "vin is required")
    private String vin;

    @NotNull(message = "status is required")
    private VehicleStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate lastMaintenanceDate;

    /**
     * Default constructor for Jackson deserialization.
     */
    public RegisterVehicleRequestDto() {
    }

    /**
     * Creates a new register vehicle request DTO.
     *
     * @param unitId the unit ID
     * @param vehicleType the vehicle type
     * @param licensePlate the license plate
     * @param vin the VIN
     * @param status the vehicle status
     * @param lastMaintenanceDate the last maintenance date (optional)
     */
    public RegisterVehicleRequestDto(String unitId, VehicleType vehicleType, String licensePlate,
                                    String vin, VehicleStatus status, LocalDate lastMaintenanceDate) {
        this.unitId = unitId;
        this.vehicleType = vehicleType;
        this.licensePlate = licensePlate;
        this.vin = vin;
        this.status = status;
        this.lastMaintenanceDate = lastMaintenanceDate;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
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
