package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for vehicle status change operations.
 * Contains unitId and status information.
 */
public class VehicleStatusResponseDto {

    private String unitId;
    private String status;

    /**
     * Default constructor for Jackson serialization.
     */
    public VehicleStatusResponseDto() {
    }

    /**
     * Creates a new vehicle status response DTO.
     *
     * @param unitId the unit ID
     * @param status the status
     */
    public VehicleStatusResponseDto(String unitId, String status) {
        this.unitId = unitId;
        this.status = status;
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
