package com.knowit.policesystem.edge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for linking a call to a dispatch.
 */
public class LinkCallToDispatchRequestDto {

    @NotBlank(message = "dispatchId is required")
    private String dispatchId;

    /**
     * Default constructor for Jackson deserialization.
     */
    public LinkCallToDispatchRequestDto() {
    }

    /**
     * Creates a new link call to dispatch request DTO.
     *
     * @param dispatchId the dispatch ID
     */
    public LinkCallToDispatchRequestDto(String dispatchId) {
        this.dispatchId = dispatchId;
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }
}
