package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for unit status change operations.
 * Matches the UnitStatusResponse schema in the OpenAPI specification.
 */
public class UnitStatusResponseDto {

    private String unitId;
    private String status;

    /**
     * Default constructor for Jackson serialization.
     */
    public UnitStatusResponseDto() {
    }

    /**
     * Creates a new unit status response DTO.
     *
     * @param unitId the unit ID
     * @param status the status
     */
    public UnitStatusResponseDto(String unitId, String status) {
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
