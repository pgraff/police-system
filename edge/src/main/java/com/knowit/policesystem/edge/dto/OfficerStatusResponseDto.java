package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for officer status change operations.
 * Matches the OfficerStatusResponse schema in the OpenAPI specification.
 */
public class OfficerStatusResponseDto {

    private String badgeNumber;
    private String status;

    /**
     * Default constructor for Jackson serialization.
     */
    public OfficerStatusResponseDto() {
    }

    /**
     * Creates a new officer status response DTO.
     *
     * @param badgeNumber the badge number
     * @param status the status
     */
    public OfficerStatusResponseDto(String badgeNumber, String status) {
        this.badgeNumber = badgeNumber;
        this.status = status;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
