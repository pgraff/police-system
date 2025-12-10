package com.knowit.policesystem.edge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for linking an activity to an incident.
 */
public class LinkActivityToIncidentRequestDto {

    @NotBlank(message = "incidentId is required")
    private String incidentId;

    /**
     * Default constructor for Jackson deserialization.
     */
    public LinkActivityToIncidentRequestDto() {
    }

    /**
     * Creates a new link activity to incident request DTO.
     *
     * @param incidentId the incident ID
     */
    public LinkActivityToIncidentRequestDto(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }
}
