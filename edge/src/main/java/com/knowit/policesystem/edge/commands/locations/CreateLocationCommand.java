package com.knowit.policesystem.edge.commands.locations;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.LocationType;
import com.knowit.policesystem.edge.dto.CreateLocationRequestDto;

/**
 * Command for creating a location.
 * This command is processed by CreateLocationCommandHandler.
 */
public class CreateLocationCommand extends Command {

    private String locationId;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private Double latitude;
    private Double longitude;
    private LocationType locationType;

    /**
     * Default constructor for deserialization.
     */
    public CreateLocationCommand() {
        super();
    }

    /**
     * Creates a new create location command from a DTO.
     *
     * @param aggregateId the aggregate identifier (locationId)
     * @param dto the request DTO containing location data
     */
    public CreateLocationCommand(String aggregateId, CreateLocationRequestDto dto) {
        super(aggregateId);
        this.locationId = dto.getLocationId();
        this.address = dto.getAddress();
        this.city = dto.getCity();
        this.state = dto.getState();
        this.zipCode = dto.getZipCode();
        this.latitude = dto.getLatitude();
        this.longitude = dto.getLongitude();
        this.locationType = dto.getLocationType();
    }

    @Override
    public String getCommandType() {
        return "CreateLocationCommand";
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
