package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.knowit.policesystem.edge.domain.IncidentStatus;
import com.knowit.policesystem.edge.domain.IncidentType;
import com.knowit.policesystem.edge.domain.Priority;
import com.knowit.policesystem.edge.util.FlexibleInstantDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for reporting an incident.
 * Matches the ReportIncidentRequest schema in the OpenAPI specification.
 */
public class ReportIncidentRequestDto {

    @NotBlank(message = "incidentId is required")
    private String incidentId;

    private String incidentNumber;

    @NotNull(message = "priority is required")
    private Priority priority;

    @NotNull(message = "status is required")
    private IncidentStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    @JsonDeserialize(using = FlexibleInstantDeserializer.class)
    private Instant reportedTime;

    private String description;

    @NotNull(message = "incidentType is required")
    private IncidentType incidentType;

    /**
     * Default constructor for Jackson deserialization.
     */
    public ReportIncidentRequestDto() {
    }

    /**
     * Creates a new report incident request DTO.
     *
     * @param incidentId the incident identifier
     * @param incidentNumber the incident number
     * @param priority the priority level
     * @param status the incident status
     * @param reportedTime the time the incident was reported
     * @param description the incident description
     * @param incidentType the type of incident
     */
    public ReportIncidentRequestDto(String incidentId, String incidentNumber, Priority priority,
                                    IncidentStatus status, Instant reportedTime, String description,
                                    IncidentType incidentType) {
        this.incidentId = incidentId;
        this.incidentNumber = incidentNumber;
        this.priority = priority;
        this.status = status;
        this.reportedTime = reportedTime;
        this.description = description;
        this.incidentType = incidentType;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public void setIncidentNumber(String incidentNumber) {
        this.incidentNumber = incidentNumber;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public Instant getReportedTime() {
        return reportedTime;
    }

    public void setReportedTime(Instant reportedTime) {
        this.reportedTime = reportedTime;
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
