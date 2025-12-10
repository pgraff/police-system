package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.AssignmentStatus;
import com.knowit.policesystem.edge.domain.AssignmentType;
import com.knowit.policesystem.edge.dto.CreateAssignmentRequestDto;

import java.time.Instant;

/**
 * Command for creating an assignment.
 * This command is processed by CreateAssignmentCommandHandler.
 */
public class CreateAssignmentCommand extends Command {

    private String assignmentId;
    private Instant assignedTime;
    private AssignmentType assignmentType;
    private AssignmentStatus status;
    private String incidentId;
    private String callId;

    /**
     * Default constructor for deserialization.
     */
    public CreateAssignmentCommand() {
        super();
    }

    /**
     * Creates a new create assignment command from a DTO.
     *
     * @param aggregateId the aggregate identifier (assignmentId)
     * @param dto the request DTO containing assignment data
     */
    public CreateAssignmentCommand(String aggregateId, CreateAssignmentRequestDto dto) {
        super(aggregateId);
        this.assignmentId = dto.getAssignmentId();
        this.assignedTime = dto.getAssignedTime();
        this.assignmentType = dto.getAssignmentType();
        this.status = dto.getStatus();
        this.incidentId = dto.getIncidentId();
        this.callId = dto.getCallId();
    }

    @Override
    public String getCommandType() {
        return "CreateAssignmentCommand";
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
