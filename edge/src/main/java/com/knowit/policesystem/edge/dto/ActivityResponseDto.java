package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for activity operations.
 * Matches the ActivityResponse schema in the OpenAPI specification.
 */
public class ActivityResponseDto {

    private String activityId;

    /**
     * Default constructor for Jackson serialization.
     */
    public ActivityResponseDto() {
    }

    /**
     * Creates a new activity response DTO.
     *
     * @param activityId the activity identifier
     */
    public ActivityResponseDto(String activityId) {
        this.activityId = activityId;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }
}
