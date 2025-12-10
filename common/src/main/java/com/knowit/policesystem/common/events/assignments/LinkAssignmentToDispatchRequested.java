package com.knowit.policesystem.common.events.assignments;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to link an assignment to a dispatch.
 * This event is published to Kafka when an assignment is linked to a dispatch via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class LinkAssignmentToDispatchRequested extends Event {

    private String assignmentId;
    private String dispatchId;

    /**
     * Default constructor for deserialization.
     */
    public LinkAssignmentToDispatchRequested() {
        super();
    }

    /**
     * Creates a new LinkAssignmentToDispatchRequested event.
     *
     * @param aggregateId the aggregate identifier (assignmentId)
     * @param assignmentId the assignment ID
     * @param dispatchId the dispatch ID
     */
    public LinkAssignmentToDispatchRequested(String aggregateId, String assignmentId, String dispatchId) {
        super(aggregateId);
        this.assignmentId = assignmentId;
        this.dispatchId = dispatchId;
    }

    @Override
    public String getEventType() {
        return "LinkAssignmentToDispatchRequested";
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }
}
