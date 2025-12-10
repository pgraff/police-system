package com.knowit.policesystem.common.events.activities;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to update an activity.
 * Published when the Update Activity endpoint is called.
 */
public class UpdateActivityRequested extends Event {

    private String activityId;
    private String description;

    /**
     * Default constructor for deserialization.
     */
    public UpdateActivityRequested() {
        super();
    }

    /**
     * Creates a new UpdateActivityRequested event.
     *
     * @param activityId  the activity identifier (aggregate ID)
     * @param description optional description
     */
    public UpdateActivityRequested(String activityId, String description) {
        super(activityId);
        this.activityId = activityId;
        this.description = description;
    }

    @Override
    public String getEventType() {
        return "UpdateActivityRequested";
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
