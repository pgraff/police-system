package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for dispatch operations.
 * Matches the DispatchResponse schema in the OpenAPI specification.
 */
public class DispatchResponseDto {

    private String dispatchId;

    /**
     * Default constructor for Jackson deserialization.
     */
    public DispatchResponseDto() {
    }

    /**
     * Creates a new dispatch response DTO.
     *
     * @param dispatchId the dispatch ID
     */
    public DispatchResponseDto(String dispatchId) {
        this.dispatchId = dispatchId;
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }
}
