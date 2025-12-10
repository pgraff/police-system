package com.knowit.policesystem.common.events.activities;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to start an activity.
 * This event is published to Kafka when an activity is started via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class StartActivityRequested extends Event {

    private String activityId;
    private Instant activityTime;
    private String activityType;
    private String description;
    private String status;

    /**
     * Default constructor for deserialization.
     */
    public StartActivityRequested() {
        super();
    }

    /**
     * Creates a new StartActivityRequested event.
     *
     * @param activityId the activity identifier (used as aggregateId)
     * @param activityTime the activity time
     * @param activityType the activity type as string
     * @param description the description
     * @param status the status as string
     */
    public StartActivityRequested(String activityId, Instant activityTime, String activityType,
                                 String description, String status) {
        super(activityId);
        this.activityId = activityId;
        this.activityTime = activityTime;
        this.activityType = activityType;
        this.description = description;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "StartActivityRequested";
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

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
