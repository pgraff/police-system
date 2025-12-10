package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.LinkAssignmentToDispatchRequestDto;

/**
 * Command for linking an assignment to a dispatch.
 */
public class LinkAssignmentToDispatchCommand extends Command {

    private String assignmentId;
    private String dispatchId;

    /**
     * Default constructor for deserialization.
     */
    public LinkAssignmentToDispatchCommand() {
        super();
    }

    /**
     * Creates a new link assignment to dispatch command.
     *
     * @param aggregateId the aggregate identifier (assignmentId)
     * @param assignmentId the assignment ID from the path
     * @param dto the request DTO containing dispatch link data
     */
    public LinkAssignmentToDispatchCommand(String aggregateId, String assignmentId, LinkAssignmentToDispatchRequestDto dto) {
        super(aggregateId);
        this.assignmentId = assignmentId;
        this.dispatchId = dto.getDispatchId();
    }

    @Override
    public String getCommandType() {
        return "LinkAssignmentToDispatchCommand";
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }
}
