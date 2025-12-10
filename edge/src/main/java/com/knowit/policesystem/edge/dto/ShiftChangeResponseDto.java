package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for shift change operations.
 * Matches the ShiftChangeResponse schema in the OpenAPI specification.
 */
public class ShiftChangeResponseDto {

    private String shiftChangeId;

    /**
     * Default constructor for Jackson serialization.
     */
    public ShiftChangeResponseDto() {
    }

    /**
     * Creates a new shift change response DTO.
     *
     * @param shiftChangeId the shift change identifier
     */
    public ShiftChangeResponseDto(String shiftChangeId) {
        this.shiftChangeId = shiftChangeId;
    }

    public String getShiftChangeId() {
        return shiftChangeId;
    }

    public void setShiftChangeId(String shiftChangeId) {
        this.shiftChangeId = shiftChangeId;
    }
}
