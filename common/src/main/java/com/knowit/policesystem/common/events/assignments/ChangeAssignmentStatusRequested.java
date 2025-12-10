package com.knowit.policesystem.common.events.assignments;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to change an assignment's status.
 * This event is published to Kafka when an assignment status is changed via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class ChangeAssignmentStatusRequested extends Event {

    private String assignmentId;
    private String status;

    /**
     * Default constructor for deserialization.
     */
    public ChangeAssignmentStatusRequested() {
        super();
    }

    /**
     * Creates a new ChangeAssignmentStatusRequested event.
     *
     * @param assignmentId the assignment identifier (used as aggregateId)
     * @param status the status as string
     */
    public ChangeAssignmentStatusRequested(String assignmentId, String status) {
        super(assignmentId);
        this.assignmentId = assignmentId;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "ChangeAssignmentStatusRequested";
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
