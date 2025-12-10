package com.knowit.policesystem.edge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for linking a call to an incident.
 */
public class LinkCallToIncidentRequestDto {

    @NotBlank(message = "incidentId is required")
    private String incidentId;

    /**
     * Default constructor for Jackson deserialization.
     */
    public LinkCallToIncidentRequestDto() {
    }

    /**
     * Creates a new link call to incident request DTO.
     *
     * @param incidentId the incident ID
     */
    public LinkCallToIncidentRequestDto(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }
}
