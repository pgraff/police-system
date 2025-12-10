package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.CompleteAssignmentRequestDto;

import java.time.Instant;

/**
 * Command for completing an assignment.
 * This command is processed by CompleteAssignmentCommandHandler.
 */
public class CompleteAssignmentCommand extends Command {

    private String assignmentId;
    private Instant completedTime;

    /**
     * Default constructor for deserialization.
     */
    public CompleteAssignmentCommand() {
        super();
    }

    /**
     * Creates a new complete assignment command from a DTO.
     *
     * @param aggregateId the aggregate identifier (assignmentId)
     * @param dto the request DTO containing completion data
     */
    public CompleteAssignmentCommand(String aggregateId, CompleteAssignmentRequestDto dto) {
        super(aggregateId);
        this.assignmentId = aggregateId;
        this.completedTime = dto.getCompletedTime();
    }

    @Override
    public String getCommandType() {
        return "CompleteAssignmentCommand";
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Instant getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Instant completedTime) {
        this.completedTime = completedTime;
    }
}
