package com.knowit.policesystem.edge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for linking an assignment to a dispatch.
 */
public class LinkAssignmentToDispatchRequestDto {

    @NotBlank(message = "dispatchId is required")
    private String dispatchId;

    /**
     * Default constructor for Jackson deserialization.
     */
    public LinkAssignmentToDispatchRequestDto() {
    }

    /**
     * Creates a new link assignment to dispatch request DTO.
     *
     * @param dispatchId the dispatch ID
     */
    public LinkAssignmentToDispatchRequestDto(String dispatchId) {
        this.dispatchId = dispatchId;
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }
}
