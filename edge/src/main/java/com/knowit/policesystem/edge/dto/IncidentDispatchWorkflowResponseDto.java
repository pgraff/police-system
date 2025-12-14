package com.knowit.policesystem.edge.dto;

import java.util.List;

/**
 * Response DTO for incident dispatch workflow.
 * Contains all created resource IDs for UI convenience.
 */
public class IncidentDispatchWorkflowResponseDto {

    private String incidentId;
    private String dispatchId;
    private String assignmentId;
    private List<String> resourceAssignmentIds;

    /**
     * Default constructor for Jackson serialization.
     */
    public IncidentDispatchWorkflowResponseDto() {
    }

    /**
     * Creates a new incident dispatch workflow response DTO.
     *
     * @param incidentId the incident identifier
     * @param dispatchId the dispatch identifier
     * @param assignmentId the assignment identifier
     * @param resourceAssignmentIds the list of resource assignment identifiers
     */
    public IncidentDispatchWorkflowResponseDto(String incidentId, String dispatchId, String assignmentId,
                                               List<String> resourceAssignmentIds) {
        this.incidentId = incidentId;
        this.dispatchId = dispatchId;
        this.assignmentId = assignmentId;
        this.resourceAssignmentIds = resourceAssignmentIds;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public List<String> getResourceAssignmentIds() {
        return resourceAssignmentIds;
    }

    public void setResourceAssignmentIds(List<String> resourceAssignmentIds) {
        this.resourceAssignmentIds = resourceAssignmentIds;
    }
}
