package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.ResourceAssignmentStatus;
import com.knowit.policesystem.edge.dto.ChangeResourceAssignmentStatusRequestDto;

/**
 * Command for changing a resource assignment's status.
 * This command is processed by ChangeResourceAssignmentStatusCommandHandler.
 */
public class ChangeResourceAssignmentStatusCommand extends Command {

    private String assignmentId;
    private String resourceId;
    private ResourceAssignmentStatus status;

    /**
     * Default constructor for deserialization.
     */
    public ChangeResourceAssignmentStatusCommand() {
        super();
    }

    /**
     * Creates a new change resource assignment status command from path variables and a DTO.
     *
     * @param aggregateId the aggregate identifier (assignmentId)
     * @param assignmentId the assignment identifier from the path
     * @param resourceId the resource identifier from the path
     * @param dto the request DTO containing status data
     */
    public ChangeResourceAssignmentStatusCommand(String aggregateId, String assignmentId, String resourceId, ChangeResourceAssignmentStatusRequestDto dto) {
        super(aggregateId);
        this.assignmentId = assignmentId;
        this.resourceId = resourceId;
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "ChangeResourceAssignmentStatusCommand";
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public ResourceAssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceAssignmentStatus status) {
        this.status = status;
    }
}
