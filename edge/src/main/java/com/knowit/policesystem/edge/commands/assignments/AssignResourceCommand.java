package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.ResourceType;
import com.knowit.policesystem.edge.domain.RoleType;
import com.knowit.policesystem.edge.dto.AssignResourceRequestDto;

import java.time.Instant;

/**
 * Command for assigning a resource to an assignment.
 * This command is processed by AssignResourceCommandHandler.
 */
public class AssignResourceCommand extends Command {

    private String assignmentId;
    private String resourceId;
    private ResourceType resourceType;
    private RoleType roleType;
    private String status;
    private Instant startTime;

    /**
     * Default constructor for deserialization.
     */
    public AssignResourceCommand() {
        super();
    }

    /**
     * Creates a new assign resource command from a DTO.
     *
     * @param aggregateId the aggregate identifier (assignmentId)
     * @param assignmentId the assignment identifier from the path
     * @param dto the request DTO containing resource assignment data
     */
    public AssignResourceCommand(String aggregateId, String assignmentId, AssignResourceRequestDto dto) {
        super(aggregateId);
        this.assignmentId = assignmentId;
        this.resourceId = dto.getResourceId();
        this.resourceType = dto.getResourceType();
        this.roleType = dto.getRoleType();
        this.status = dto.getStatus();
        this.startTime = dto.getStartTime();
    }

    @Override
    public String getCommandType() {
        return "AssignResourceCommand";
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

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
}
