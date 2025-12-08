package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for unit operations.
 * Matches the UnitResponse schema in the OpenAPI specification.
 */
public class UnitResponseDto {

    private String unitId;

    /**
     * Default constructor for Jackson deserialization.
     */
    public UnitResponseDto() {
    }

    /**
     * Creates a new unit response DTO.
     *
     * @param unitId the unit ID
     */
    public UnitResponseDto(String unitId) {
        this.unitId = unitId;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }
}
