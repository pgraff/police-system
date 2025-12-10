package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for linking an activity to an incident.
 */
public class LinkActivityToIncidentResponseDto {

    private String activityId;
    private String incidentId;

    /**
     * Default constructor for Jackson serialization.
     */
    public LinkActivityToIncidentResponseDto() {
    }

    /**
     * Creates a new link activity to incident response DTO.
     *
     * @param activityId the activity identifier
     * @param incidentId the incident identifier
     */
    public LinkActivityToIncidentResponseDto(String activityId, String incidentId) {
        this.activityId = activityId;
        this.incidentId = incidentId;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }
}
