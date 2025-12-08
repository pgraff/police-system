package com.knowit.policesystem.edge.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing a vehicle's status.
 * Matches the ChangeStatusRequest schema in the OpenAPI specification.
 */
public class ChangeVehicleStatusRequestDto {

    @NotNull(message = "status is required")
    private String status;

    /**
     * Default constructor for Jackson deserialization.
     */
    public ChangeVehicleStatusRequestDto() {
    }

    /**
     * Creates a new change vehicle status request DTO.
     *
     * @param status the new status (required)
     */
    public ChangeVehicleStatusRequestDto(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
