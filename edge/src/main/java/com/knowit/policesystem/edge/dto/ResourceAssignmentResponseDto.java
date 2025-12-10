package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for resource assignment operations.
 * Matches the ResourceAssignmentResponse schema in the OpenAPI specification.
 */
public class ResourceAssignmentResponseDto {

    private String resourceAssignmentId;

    /**
     * Default constructor for Jackson deserialization.
     */
    public ResourceAssignmentResponseDto() {
    }

    /**
     * Creates a new resource assignment response DTO.
     *
     * @param resourceAssignmentId the resource assignment identifier
     */
    public ResourceAssignmentResponseDto(String resourceAssignmentId) {
        this.resourceAssignmentId = resourceAssignmentId;
    }

    public String getResourceAssignmentId() {
        return resourceAssignmentId;
    }

    public void setResourceAssignmentId(String resourceAssignmentId) {
        this.resourceAssignmentId = resourceAssignmentId;
    }
}
