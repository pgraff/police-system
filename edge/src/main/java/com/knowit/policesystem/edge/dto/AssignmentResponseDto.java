package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for assignment operations.
 * Matches the AssignmentResponse schema in the OpenAPI specification.
 */
public class AssignmentResponseDto {

    private String assignmentId;
    private RelatedResourcesDto relatedResources;

    /**
     * Default constructor for Jackson serialization.
     */
    public AssignmentResponseDto() {
    }

    /**
     * Creates a new assignment response DTO.
     *
     * @param assignmentId the assignment identifier
     */
    public AssignmentResponseDto(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public RelatedResourcesDto getRelatedResources() {
        return relatedResources;
    }

    public void setRelatedResources(RelatedResourcesDto relatedResources) {
        this.relatedResources = relatedResources;
    }
}
