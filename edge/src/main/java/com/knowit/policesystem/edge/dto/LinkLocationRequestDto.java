package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.domain.LocationRoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for linking a location to an incident or call.
 */
public class LinkLocationRequestDto {

    @NotBlank(message = "locationId is required")
    private String locationId;

    @NotNull(message = "locationRoleType is required")
    private LocationRoleType locationRoleType;

    private String description;

    /**
     * Default constructor for Jackson deserialization.
     */
    public LinkLocationRequestDto() {
    }

    /**
     * Creates a new link location request DTO.
     *
     * @param locationId the location ID
     * @param locationRoleType the location role type
     * @param description the description (optional)
     */
    public LinkLocationRequestDto(String locationId, LocationRoleType locationRoleType, String description) {
        this.locationId = locationId;
        this.locationRoleType = locationRoleType;
        this.description = description;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public LocationRoleType getLocationRoleType() {
        return locationRoleType;
    }

    public void setLocationRoleType(LocationRoleType locationRoleType) {
        this.locationRoleType = locationRoleType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
