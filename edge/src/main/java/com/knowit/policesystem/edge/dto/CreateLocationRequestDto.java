package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.domain.LocationType;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a location.
 */
public class CreateLocationRequestDto {

    @NotBlank(message = "locationId is required")
    private String locationId;

    private String address;

    private String city;

    private String state;

    private String zipCode;

    private Double latitude;

    private Double longitude;

    private LocationType locationType;

    /**
     * Default constructor for Jackson deserialization.
     */
    public CreateLocationRequestDto() {
    }

    /**
     * Creates a new create location request DTO.
     *
     * @param locationId the location ID
     * @param address the address (optional)
     * @param city the city (optional)
     * @param state the state (optional)
     * @param zipCode the zip code (optional)
     * @param latitude the latitude (optional)
     * @param longitude the longitude (optional)
     * @param locationType the location type (optional)
     */
    public CreateLocationRequestDto(String locationId, String address, String city, String state,
                                   String zipCode, Double latitude, Double longitude, LocationType locationType) {
        this.locationId = locationId;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationType = locationType;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }
}
