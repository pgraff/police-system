package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.UnassignResourceRequestDto;

import java.time.Instant;

/**
 * Command for unassigning a resource from an assignment.
 * This command is processed by UnassignResourceCommandHandler.
 */
public class UnassignResourceCommand extends Command {

    private String assignmentId;
    private String resourceId;
    private Instant endTime;

    /**
     * Default constructor for deserialization.
     */
    public UnassignResourceCommand() {
        super();
    }

    /**
     * Creates a new unassign resource command from path variables and a DTO.
     *
     * @param aggregateId the aggregate identifier (assignmentId)
     * @param assignmentId the assignment identifier from the path
     * @param resourceId the resource identifier from the path
     * @param dto the request DTO containing unassign resource data
     */
    public UnassignResourceCommand(String aggregateId, String assignmentId, String resourceId, UnassignResourceRequestDto dto) {
        super(aggregateId);
        this.assignmentId = assignmentId;
        this.resourceId = resourceId;
        this.endTime = dto.getEndTime();
    }

    @Override
    public String getCommandType() {
        return "UnassignResourceCommand";
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

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
}
