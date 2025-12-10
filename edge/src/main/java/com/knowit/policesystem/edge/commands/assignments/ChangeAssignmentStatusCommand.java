package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.AssignmentStatus;
import com.knowit.policesystem.edge.dto.ChangeAssignmentStatusRequestDto;

/**
 * Command for changing an assignment's status.
 * This command is processed by ChangeAssignmentStatusCommandHandler.
 */
public class ChangeAssignmentStatusCommand extends Command {

    private String assignmentId;
    private AssignmentStatus status;

    /**
     * Default constructor for deserialization.
     */
    public ChangeAssignmentStatusCommand() {
        super();
    }

    /**
     * Creates a new change assignment status command from a DTO.
     *
     * @param aggregateId the aggregate identifier (assignmentId)
     * @param dto the request DTO containing status data
     */
    public ChangeAssignmentStatusCommand(String aggregateId, ChangeAssignmentStatusRequestDto dto) {
        super(aggregateId);
        this.assignmentId = aggregateId;
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "ChangeAssignmentStatusCommand";
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }
}
