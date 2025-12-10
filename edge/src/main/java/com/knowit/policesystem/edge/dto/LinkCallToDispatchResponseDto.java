package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for linking a call to a dispatch.
 */
public class LinkCallToDispatchResponseDto {

    private String callId;
    private String dispatchId;

    /**
     * Default constructor for Jackson serialization.
     */
    public LinkCallToDispatchResponseDto() {
    }

    /**
     * Creates a new link call to dispatch response DTO.
     *
     * @param callId the call identifier
     * @param dispatchId the dispatch identifier
     */
    public LinkCallToDispatchResponseDto(String callId, String dispatchId) {
        this.callId = callId;
        this.dispatchId = dispatchId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }
}
