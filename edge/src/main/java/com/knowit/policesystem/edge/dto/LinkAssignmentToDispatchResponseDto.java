package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for linking an assignment to a dispatch.
 */
public class LinkAssignmentToDispatchResponseDto {

    private String assignmentId;
    private String dispatchId;

    /**
     * Default constructor for Jackson serialization.
     */
    public LinkAssignmentToDispatchResponseDto() {
    }

    /**
     * Creates a new link assignment to dispatch response DTO.
     *
     * @param assignmentId the assignment identifier
     * @param dispatchId the dispatch identifier
     */
    public LinkAssignmentToDispatchResponseDto(String assignmentId, String dispatchId) {
        this.assignmentId = assignmentId;
        this.dispatchId = dispatchId;
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
