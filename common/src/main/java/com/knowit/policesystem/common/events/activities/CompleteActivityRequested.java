package com.knowit.policesystem.common.events.activities;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to complete an activity.
 * This event is published to Kafka when an activity is completed via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class CompleteActivityRequested extends Event {

    private String activityId;
    private Instant completedTime;

    /**
     * Default constructor for deserialization.
     */
    public CompleteActivityRequested() {
        super();
    }

    /**
     * Creates a new CompleteActivityRequested event.
     *
     * @param activityId the activity identifier (used as aggregateId)
     * @param completedTime the completion time
     */
    public CompleteActivityRequested(String activityId, Instant completedTime) {
        super(activityId);
        this.activityId = activityId;
        this.completedTime = completedTime;
    }

    @Override
    public String getEventType() {
        return "CompleteActivityRequested";
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public Instant getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Instant completedTime) {
        this.completedTime = completedTime;
    }
}
