package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for call operations.
 * Matches the CallResponse schema in the OpenAPI specification.
 */
public class CallResponseDto {

    private String callId;
    private String callNumber;

    /**
     * Default constructor for Jackson serialization.
     */
    public CallResponseDto() {
    }

    /**
     * Creates a new call response DTO.
     *
     * @param callId the call identifier
     * @param callNumber the call number
     */
    public CallResponseDto(String callId, String callNumber) {
        this.callId = callId;
        this.callNumber = callNumber;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getCallNumber() {
        return callNumber;
    }

    public void setCallNumber(String callNumber) {
        this.callNumber = callNumber;
    }
}
