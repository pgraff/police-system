package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for officer shift operations.
 * Matches the OfficerShiftResponse schema in the OpenAPI specification.
 */
public class OfficerShiftResponseDto {

    private String shiftId;
    private String badgeNumber;

    /**
     * Default constructor for Jackson serialization.
     */
    public OfficerShiftResponseDto() {
    }

    /**
     * Creates a new officer shift response DTO.
     *
     * @param shiftId the shift identifier
     * @param badgeNumber the badge number
     */
    public OfficerShiftResponseDto(String shiftId, String badgeNumber) {
        this.shiftId = shiftId;
        this.badgeNumber = badgeNumber;
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }
}
