package com.knowit.policesystem.common.events.activities;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to change an activity's status.
 * This event is published to Kafka when an activity status is changed via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class ChangeActivityStatusRequested extends Event {

    private String activityId;
    private String status;

    /**
     * Default constructor for deserialization.
     */
    public ChangeActivityStatusRequested() {
        super();
    }

    /**
     * Creates a new ChangeActivityStatusRequested event.
     *
     * @param activityId the activity identifier (used as aggregateId)
     * @param status the status as string
     */
    public ChangeActivityStatusRequested(String activityId, String status) {
        super(activityId);
        this.activityId = activityId;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "ChangeActivityStatusRequested";
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
