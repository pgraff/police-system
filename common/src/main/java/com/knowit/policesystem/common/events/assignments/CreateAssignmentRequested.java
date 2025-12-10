package com.knowit.policesystem.common.events.assignments;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to create an assignment.
 * This event is published to Kafka when an assignment is created via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class CreateAssignmentRequested extends Event {

    private String assignmentId;
    private Instant assignedTime;
    private String assignmentType;
    private String status;
    private String incidentId;
    private String callId;

    /**
     * Default constructor for deserialization.
     */
    public CreateAssignmentRequested() {
        super();
    }

    /**
     * Creates a new CreateAssignmentRequested event.
     *
     * @param aggregateId the aggregate identifier (assignmentId)
     * @param assignmentId the assignment identifier
     * @param assignedTime the assigned time
     * @param assignmentType the assignment type as string
     * @param status the status as string
     * @param incidentId the incident identifier (optional, mutually exclusive with callId)
     * @param callId the call identifier (optional, mutually exclusive with incidentId)
     */
    public CreateAssignmentRequested(String aggregateId, String assignmentId, Instant assignedTime,
                                     String assignmentType, String status, String incidentId, String callId) {
        super(aggregateId);
        this.assignmentId = assignmentId;
        this.assignedTime = assignedTime;
        this.assignmentType = assignmentType;
        this.status = status;
        this.incidentId = incidentId;
        this.callId = callId;
    }

    @Override
    public String getEventType() {
        return "CreateAssignmentRequested";
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Instant getAssignedTime() {
        return assignedTime;
    }

    public void setAssignedTime(Instant assignedTime) {
        this.assignedTime = assignedTime;
    }

    public String getAssignmentType() {
        return assignmentType;
    }

    public void setAssignmentType(String assignmentType) {
        this.assignmentType = assignmentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }
}
