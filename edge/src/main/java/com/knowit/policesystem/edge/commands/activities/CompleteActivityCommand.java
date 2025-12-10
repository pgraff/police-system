package com.knowit.policesystem.edge.commands.activities;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.CompleteActivityRequestDto;

import java.time.Instant;

/**
 * Command for completing an activity.
 * This command is processed by CompleteActivityCommandHandler.
 */
public class CompleteActivityCommand extends Command {

    private String activityId;
    private Instant completedTime;

    /**
     * Default constructor for deserialization.
     */
    public CompleteActivityCommand() {
        super();
    }

    /**
     * Creates a new complete activity command from a DTO.
     *
     * @param aggregateId the aggregate identifier (activityId)
     * @param dto the request DTO containing completion data
     */
    public CompleteActivityCommand(String aggregateId, CompleteActivityRequestDto dto) {
        super(aggregateId);
        this.activityId = aggregateId;
        this.completedTime = dto.getCompletedTime();
    }

    @Override
    public String getCommandType() {
        return "CompleteActivityCommand";
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public Instant getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Instant completedTime) {
        this.completedTime = completedTime;
    }
}
