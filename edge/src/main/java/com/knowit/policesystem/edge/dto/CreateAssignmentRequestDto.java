package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.knowit.policesystem.edge.domain.AssignmentStatus;
import com.knowit.policesystem.edge.domain.AssignmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for creating an assignment.
 * Matches the CreateAssignmentRequest schema in the OpenAPI specification.
 */
public class CreateAssignmentRequestDto {

    @NotBlank(message = "assignmentId is required")
    private String assignmentId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant assignedTime;

    @NotNull(message = "assignmentType is required")
    private AssignmentType assignmentType;

    @NotNull(message = "status is required")
    private AssignmentStatus status;

    private String incidentId;

    private String callId;

    /**
     * Default constructor for Jackson deserialization.
     */
    public CreateAssignmentRequestDto() {
    }

    /**
     * Creates a new create assignment request DTO.
     *
     * @param assignmentId the assignment identifier
     * @param assignedTime the assigned time
     * @param assignmentType the type of assignment
     * @param status the assignment status
     * @param incidentId the incident identifier (optional, mutually exclusive with callId)
     * @param callId the call identifier (optional, mutually exclusive with incidentId)
     */
    public CreateAssignmentRequestDto(String assignmentId, Instant assignedTime, AssignmentType assignmentType,
                                     AssignmentStatus status, String incidentId, String callId) {
        this.assignmentId = assignmentId;
        this.assignedTime = assignedTime;
        this.assignmentType = assignmentType;
        this.status = status;
        this.incidentId = incidentId;
        this.callId = callId;
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Instant getAssignedTime() {
        return assignedTime;
    }

    public void setAssignedTime(Instant assignedTime) {
        this.assignedTime = assignedTime;
    }

    public AssignmentType getAssignmentType() {
        return assignmentType;
    }

    public void setAssignmentType(AssignmentType assignmentType) {
        this.assignmentType = assignmentType;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }
}
