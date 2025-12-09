package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.knowit.policesystem.edge.domain.CallStatus;
import com.knowit.policesystem.edge.domain.CallType;
import com.knowit.policesystem.edge.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for receiving a call.
 * Matches the ReceiveCallRequest schema in the OpenAPI specification.
 */
public class ReceiveCallRequestDto {

    @NotBlank(message = "callId is required")
    private String callId;

    private String callNumber;

    @NotNull(message = "priority is required")
    private Priority priority;

    @NotNull(message = "status is required")
    private CallStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant receivedTime;

    private String description;

    @NotNull(message = "callType is required")
    private CallType callType;

    /**
     * Default constructor for Jackson deserialization.
     */
    public ReceiveCallRequestDto() {
    }

    /**
     * Creates a new receive call request DTO.
     *
     * @param callId the call identifier
     * @param callNumber the call number
     * @param priority the priority level
     * @param status the call status
     * @param receivedTime the time the call was received
     * @param description the call description
     * @param callType the type of call
     */
    public ReceiveCallRequestDto(String callId, String callNumber, Priority priority,
                                 CallStatus status, Instant receivedTime, String description,
                                 CallType callType) {
        this.callId = callId;
        this.callNumber = callNumber;
        this.priority = priority;
        this.status = status;
        this.receivedTime = receivedTime;
        this.description = description;
        this.callType = callType;
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

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
    }

    public Instant getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(Instant receivedTime) {
        this.receivedTime = receivedTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(CallType callType) {
        this.callType = callType;
    }
}
