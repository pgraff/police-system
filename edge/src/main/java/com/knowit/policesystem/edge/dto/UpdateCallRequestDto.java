package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.knowit.policesystem.edge.domain.CallType;
import com.knowit.policesystem.edge.domain.Priority;
import jakarta.validation.constraints.AssertTrue;

/**
 * Request DTO for updating a call.
 * All fields are optional, but at least one must be provided.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateCallRequestDto {

    private Priority priority;

    private String description;

    private CallType callType;

    public UpdateCallRequestDto() {
    }

    public UpdateCallRequestDto(Priority priority, String description, CallType callType) {
        this.priority = priority;
        this.description = description;
        this.callType = callType;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
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

    @AssertTrue(message = "At least one of priority, description, or callType must be provided")
    public boolean isAtLeastOneFieldProvided() {
        return priority != null
                || (description != null && !description.trim().isEmpty())
                || callType != null;
    }
}
