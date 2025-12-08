package com.knowit.policesystem.edge.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing an officer's status.
 * Matches the ChangeStatusRequest schema in the OpenAPI specification.
 */
public class ChangeOfficerStatusRequestDto {

    @NotNull(message = "status is required")
    private String status;

    /**
     * Default constructor for Jackson deserialization.
     */
    public ChangeOfficerStatusRequestDto() {
    }

    /**
     * Creates a new change officer status request DTO.
     *
     * @param status the new status (required)
     */
    public ChangeOfficerStatusRequestDto(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
