package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for location operations.
 */
public class LocationResponseDto {

    private String locationId;

    /**
     * Default constructor for Jackson serialization.
     */
    public LocationResponseDto() {
    }

    /**
     * Creates a new location response DTO.
     *
     * @param locationId the location ID
     */
    public LocationResponseDto(String locationId) {
        this.locationId = locationId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }
}
