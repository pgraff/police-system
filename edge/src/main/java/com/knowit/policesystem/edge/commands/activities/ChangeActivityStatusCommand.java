package com.knowit.policesystem.edge.commands.activities;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.ActivityStatus;
import com.knowit.policesystem.edge.dto.ChangeActivityStatusRequestDto;

/**
 * Command for changing an activity's status.
 * This command is processed by ChangeActivityStatusCommandHandler.
 */
public class ChangeActivityStatusCommand extends Command {

    private String activityId;
    private ActivityStatus status;

    /**
     * Default constructor for deserialization.
     */
    public ChangeActivityStatusCommand() {
        super();
    }

    /**
     * Creates a new change activity status command from a DTO.
     *
     * @param aggregateId the aggregate identifier (activityId)
     * @param dto the request DTO containing status data
     */
    public ChangeActivityStatusCommand(String aggregateId, ChangeActivityStatusRequestDto dto) {
        super(aggregateId);
        this.activityId = aggregateId;
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "ChangeActivityStatusCommand";
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public void setStatus(ActivityStatus status) {
        this.status = status;
    }
}
