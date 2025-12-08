package com.knowit.policesystem.common.events.locations;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to update a location.
 * This event is published to Kafka and NATS/JetStream when a location is updated via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 * All fields are nullable to support partial updates - null fields mean "don't update this field".
 */
public class UpdateLocationRequested extends Event {

    private String locationId;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String latitude;
    private String longitude;
    private String locationType;

    /**
     * Default constructor for deserialization.
     */
    public UpdateLocationRequested() {
        super();
    }

    /**
     * Creates a new UpdateLocationRequested event.
     *
     * @param locationId the location ID (used as aggregateId)
     * @param address the address (nullable for partial updates)
     * @param city the city (nullable for partial updates)
     * @param state the state (nullable for partial updates)
     * @param zipCode the zip code (nullable for partial updates)
     * @param latitude the latitude as string (nullable for partial updates)
     * @param longitude the longitude as string (nullable for partial updates)
     * @param locationType the location type as string enum name (nullable for partial updates)
     */
    public UpdateLocationRequested(String locationId, String address, String city, String state,
                                   String zipCode, String latitude, String longitude, String locationType) {
        super(locationId);
        this.locationId = locationId;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationType = locationType;
    }

    @Override
    public String getEventType() {
        return "UpdateLocationRequested";
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

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }
}
