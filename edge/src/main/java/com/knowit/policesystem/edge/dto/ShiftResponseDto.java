package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for shift operations.
 * Matches the ShiftResponse schema in the OpenAPI specification.
 */
public class ShiftResponseDto {

    private String shiftId;

    /**
     * Default constructor for Jackson serialization.
     */
    public ShiftResponseDto() {
    }

    /**
     * Creates a new shift response DTO.
     *
     * @param shiftId the shift identifier
     */
    public ShiftResponseDto(String shiftId) {
        this.shiftId = shiftId;
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }
}
