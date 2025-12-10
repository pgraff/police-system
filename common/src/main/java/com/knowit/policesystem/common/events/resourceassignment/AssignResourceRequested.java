package com.knowit.policesystem.common.events.resourceassignment;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to assign a resource to an assignment.
 * This event is published to Kafka when a resource is assigned to an assignment via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class AssignResourceRequested extends Event {

    private String assignmentId;
    private String resourceId;
    private String resourceType;
    private String roleType;
    private String status;
    private Instant startTime;

    /**
     * Default constructor for deserialization.
     */
    public AssignResourceRequested() {
        super();
    }

    /**
     * Creates a new AssignResourceRequested event.
     *
     * @param aggregateId the aggregate identifier (assignmentId)
     * @param assignmentId the assignment identifier
     * @param resourceId the resource identifier
     * @param resourceType the resource type as string enum name
     * @param roleType the role type as string enum name
     * @param status the status
     * @param startTime the start time
     */
    public AssignResourceRequested(String aggregateId, String assignmentId, String resourceId,
                                   String resourceType, String roleType, String status, Instant startTime) {
        super(aggregateId);
        this.assignmentId = assignmentId;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.roleType = roleType;
        this.status = status;
        this.startTime = startTime;
    }

    @Override
    public String getEventType() {
        return "AssignResourceRequested";
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

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
}
