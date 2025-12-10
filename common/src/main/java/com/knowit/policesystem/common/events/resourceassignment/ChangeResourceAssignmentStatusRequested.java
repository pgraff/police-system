package com.knowit.policesystem.common.events.resourceassignment;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to change a resource assignment's status.
 * This event is published to Kafka when a resource assignment status is changed via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class ChangeResourceAssignmentStatusRequested extends Event {

    private String assignmentId;
    private String resourceId;
    private String status;

    /**
     * Default constructor for deserialization.
     */
    public ChangeResourceAssignmentStatusRequested() {
        super();
    }

    /**
     * Creates a new ChangeResourceAssignmentStatusRequested event.
     *
     * @param aggregateId the aggregate identifier (assignmentId)
     * @param assignmentId the assignment identifier
     * @param resourceId the resource identifier
     * @param status the status as string
     */
    public ChangeResourceAssignmentStatusRequested(String aggregateId, String assignmentId, String resourceId, String status) {
        super(aggregateId);
        this.assignmentId = assignmentId;
        this.resourceId = resourceId;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "ChangeResourceAssignmentStatusRequested";
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
