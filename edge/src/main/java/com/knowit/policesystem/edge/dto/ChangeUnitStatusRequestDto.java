package com.knowit.policesystem.edge.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing a unit's status.
 * Matches the ChangeStatusRequest schema in the OpenAPI specification.
 */
public class ChangeUnitStatusRequestDto {

    @NotNull(message = "status is required")
    private String status;

    /**
     * Default constructor for Jackson deserialization.
     */
    public ChangeUnitStatusRequestDto() {
    }

    /**
     * Creates a new change unit status request DTO.
     *
     * @param status the new status (required)
     */
    public ChangeUnitStatusRequestDto(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
