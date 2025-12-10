package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.knowit.policesystem.edge.domain.ActivityStatus;
import com.knowit.policesystem.edge.domain.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for starting an activity.
 * Matches the StartActivityRequest schema in the OpenAPI specification.
 */
public class StartActivityRequestDto {

    @NotBlank(message = "activityId is required")
    private String activityId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant activityTime;

    @NotNull(message = "activityType is required")
    private ActivityType activityType;

    private String description;

    @NotNull(message = "status is required")
    private ActivityStatus status;

    /**
     * Default constructor for Jackson deserialization.
     */
    public StartActivityRequestDto() {
    }

    /**
     * Creates a new start activity request DTO.
     *
     * @param activityId the activity identifier
     * @param activityTime the activity time
     * @param activityType the type of activity
     * @param description the activity description
     * @param status the activity status
     */
    public StartActivityRequestDto(String activityId, Instant activityTime, ActivityType activityType,
                                   String description, ActivityStatus status) {
        this.activityId = activityId;
        this.activityTime = activityTime;
        this.activityType = activityType;
        this.description = description;
        this.status = status;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public Instant getActivityTime() {
        return activityTime;
    }

    public void setActivityTime(Instant activityTime) {
        this.activityTime = activityTime;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public void setStatus(ActivityStatus status) {
        this.status = status;
    }
}
