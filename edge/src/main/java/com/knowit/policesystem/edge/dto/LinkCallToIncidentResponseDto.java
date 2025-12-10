package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for linking a call to an incident.
 */
public class LinkCallToIncidentResponseDto {

    private String callId;
    private String incidentId;

    /**
     * Default constructor for Jackson serialization.
     */
    public LinkCallToIncidentResponseDto() {
    }

    /**
     * Creates a new link call to incident response DTO.
     *
     * @param callId the call identifier
     * @param incidentId the incident identifier
     */
    public LinkCallToIncidentResponseDto(String callId, String incidentId) {
        this.callId = callId;
        this.incidentId = incidentId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }
}
