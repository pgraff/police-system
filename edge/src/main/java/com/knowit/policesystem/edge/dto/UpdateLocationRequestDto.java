package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.domain.LocationType;

/**
 * Request DTO for updating a location.
 * Matches the UpdateLocationRequest schema in the OpenAPI specification.
 * All fields are optional for partial updates.
 */
public class UpdateLocationRequestDto {

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
    public UpdateLocationRequestDto() {
    }

    /**
     * Creates a new update location request DTO.
     *
     * @param address the address (optional)
     * @param city the city (optional)
     * @param state the state (optional)
     * @param zipCode the zip code (optional)
     * @param latitude the latitude (optional)
     * @param longitude the longitude (optional)
     * @param locationType the location type (optional)
     */
    public UpdateLocationRequestDto(String address, String city, String state,
                                   String zipCode, Double latitude, Double longitude, LocationType locationType) {
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationType = locationType;
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
