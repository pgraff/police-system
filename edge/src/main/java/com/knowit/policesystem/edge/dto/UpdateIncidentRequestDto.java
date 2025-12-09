package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.domain.IncidentType;
import com.knowit.policesystem.edge.domain.Priority;

/**
 * Request DTO for updating an incident.
 * All fields are optional to allow partial updates.
 */
public class UpdateIncidentRequestDto {

    private Priority priority;
    private String description;
    private IncidentType incidentType;

    /** Default constructor for Jackson deserialization. */
    public UpdateIncidentRequestDto() {
    }

    /**
     * Creates an update incident request.
     *
     * @param priority the priority level (optional)
     * @param description the incident description (optional)
     * @param incidentType the type of incident (optional)
     */
    public UpdateIncidentRequestDto(Priority priority, String description, IncidentType incidentType) {
        this.priority = priority;
        this.description = description;
        this.incidentType = incidentType;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IncidentType getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(IncidentType incidentType) {
        this.incidentType = incidentType;
    }
}
