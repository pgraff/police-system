package com.knowit.policesystem.common.events.assignments;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to complete an assignment.
 * This event is published to Kafka when an assignment is completed via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class CompleteAssignmentRequested extends Event {

    private String assignmentId;
    private Instant completedTime;

    /**
     * Default constructor for deserialization.
     */
    public CompleteAssignmentRequested() {
        super();
    }

    /**
     * Creates a new CompleteAssignmentRequested event.
     *
     * @param assignmentId the assignment identifier (used as aggregateId)
     * @param completedTime the completion time
     */
    public CompleteAssignmentRequested(String assignmentId, Instant completedTime) {
        super(assignmentId);
        this.assignmentId = assignmentId;
        this.completedTime = completedTime;
    }

    @Override
    public String getEventType() {
        return "CompleteAssignmentRequested";
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Instant getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Instant completedTime) {
        this.completedTime = completedTime;
    }
}
