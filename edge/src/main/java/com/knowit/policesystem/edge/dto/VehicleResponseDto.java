package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for vehicle operations.
 * Matches the VehicleResponse schema in the OpenAPI specification.
 */
public class VehicleResponseDto {

    private String vehicleId;
    private String unitId;

    /**
     * Default constructor for Jackson serialization.
     */
    public VehicleResponseDto() {
    }

    /**
     * Creates a new vehicle response DTO.
     * The vehicleId is set to the unitId.
     *
     * @param unitId the unit ID (also used as vehicleId)
     */
    public VehicleResponseDto(String unitId) {
        this.vehicleId = unitId;
        this.unitId = unitId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }
}
