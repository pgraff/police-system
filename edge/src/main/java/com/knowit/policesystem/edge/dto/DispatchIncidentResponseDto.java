package com.knowit.policesystem.edge.dto;

import java.time.Instant;

/**
 * Response DTO for dispatch incident operations.
 * Includes both incident and dispatch identifiers for UI convenience.
 * Matches the DispatchIncidentResponse schema in the OpenAPI specification.
 */
public class DispatchIncidentResponseDto {

    private String incidentId;
    private String incidentNumber;
    private String dispatchId;
    private Instant dispatchedTime;
    private RelatedResourcesDto relatedResources;

    /**
     * Default constructor for Jackson serialization.
     */
    public DispatchIncidentResponseDto() {
    }

    /**
     * Creates a new dispatch incident response DTO.
     *
     * @param incidentId the incident identifier
     * @param incidentNumber the incident number
     * @param dispatchId the dispatch identifier
     * @param dispatchedTime the dispatched time
     */
    public DispatchIncidentResponseDto(String incidentId, String incidentNumber, String dispatchId, Instant dispatchedTime) {
        this.incidentId = incidentId;
        this.incidentNumber = incidentNumber;
        this.dispatchId = dispatchId;
        this.dispatchedTime = dispatchedTime;
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

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }

    public Instant getDispatchedTime() {
        return dispatchedTime;
    }

    public void setDispatchedTime(Instant dispatchedTime) {
        this.dispatchedTime = dispatchedTime;
    }

    public RelatedResourcesDto getRelatedResources() {
        return relatedResources;
    }

    public void setRelatedResources(RelatedResourcesDto relatedResources) {
        this.relatedResources = relatedResources;
    }
}
