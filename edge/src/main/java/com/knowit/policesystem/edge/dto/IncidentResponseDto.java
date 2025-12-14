package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for incident operations.
 * Matches the IncidentResponse schema in the OpenAPI specification.
 */
public class IncidentResponseDto {

    private String incidentId;
    private String incidentNumber;
    private RelatedResourcesDto relatedResources;

    /**
     * Default constructor for Jackson serialization.
     */
    public IncidentResponseDto() {
    }

    /**
     * Creates a new incident response DTO.
     *
     * @param incidentId the incident identifier
     * @param incidentNumber the incident number
     */
    public IncidentResponseDto(String incidentId, String incidentNumber) {
        this.incidentId = incidentId;
        this.incidentNumber = incidentNumber;
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

    public RelatedResourcesDto getRelatedResources() {
        return relatedResources;
    }

    public void setRelatedResources(RelatedResourcesDto relatedResources) {
        this.relatedResources = relatedResources;
    }
}
