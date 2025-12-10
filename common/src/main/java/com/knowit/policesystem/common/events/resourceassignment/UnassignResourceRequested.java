package com.knowit.policesystem.common.events.resourceassignment;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to unassign a resource from an assignment.
 * This event is published to Kafka when a resource is unassigned from an assignment via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class UnassignResourceRequested extends Event {

    private String assignmentId;
    private String resourceId;
    private Instant endTime;

    /**
     * Default constructor for deserialization.
     */
    public UnassignResourceRequested() {
        super();
    }

    /**
     * Creates a new UnassignResourceRequested event.
     *
     * @param aggregateId the aggregate identifier (assignmentId)
     * @param assignmentId the assignment identifier
     * @param resourceId the resource identifier
     * @param endTime the end time
     */
    public UnassignResourceRequested(String aggregateId, String assignmentId, String resourceId, Instant endTime) {
        super(aggregateId);
        this.assignmentId = assignmentId;
        this.resourceId = resourceId;
        this.endTime = endTime;
    }

    @Override
    public String getEventType() {
        return "UnassignResourceRequested";
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

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
}
