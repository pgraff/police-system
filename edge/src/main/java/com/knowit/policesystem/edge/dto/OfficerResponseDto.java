package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for officer operations.
 * Matches the OfficerResponse schema in the OpenAPI specification.
 */
public class OfficerResponseDto {

    private String officerId;
    private String badgeNumber;

    /**
     * Default constructor for Jackson serialization.
     */
    public OfficerResponseDto() {
    }

    /**
     * Creates a new officer response DTO.
     * The officerId is set to the badgeNumber.
     *
     * @param badgeNumber the badge number (also used as officerId)
     */
    public OfficerResponseDto(String badgeNumber) {
        this.officerId = badgeNumber;
        this.badgeNumber = badgeNumber;
    }

    public String getOfficerId() {
        return officerId;
    }

    public void setOfficerId(String officerId) {
        this.officerId = officerId;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }
}
