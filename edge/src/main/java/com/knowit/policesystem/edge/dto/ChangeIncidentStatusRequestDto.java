package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.domain.IncidentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing an incident's status.
 */
public class ChangeIncidentStatusRequestDto {

    @NotNull(message = "status is required")
    private IncidentStatus status;

    /** Default constructor for Jackson deserialization. */
    public ChangeIncidentStatusRequestDto() {
    }

    /**
     * Creates a change-incident-status request.
     *
     * @param status the new incident status
     */
    public ChangeIncidentStatusRequestDto(IncidentStatus status) {
        this.status = status;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }
}
