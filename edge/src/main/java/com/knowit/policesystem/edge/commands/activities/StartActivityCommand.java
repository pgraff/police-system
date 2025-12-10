package com.knowit.policesystem.edge.commands.activities;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.ActivityStatus;
import com.knowit.policesystem.edge.domain.ActivityType;
import com.knowit.policesystem.edge.dto.StartActivityRequestDto;

import java.time.Instant;

/**
 * Command for starting an activity.
 * This command is processed by StartActivityCommandHandler.
 */
public class StartActivityCommand extends Command {

    private String activityId;
    private Instant activityTime;
    private ActivityType activityType;
    private String description;
    private ActivityStatus status;

    /**
     * Default constructor for deserialization.
     */
    public StartActivityCommand() {
        super();
    }

    /**
     * Creates a new start activity command from a DTO.
     *
     * @param aggregateId the aggregate identifier (activityId)
     * @param dto the request DTO containing activity data
     */
    public StartActivityCommand(String aggregateId, StartActivityRequestDto dto) {
        super(aggregateId);
        this.activityId = dto.getActivityId();
        this.activityTime = dto.getActivityTime();
        this.activityType = dto.getActivityType();
        this.description = dto.getDescription();
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "StartActivityCommand";
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public Instant getActivityTime() {
        return activityTime;
    }

    public void setActivityTime(Instant activityTime) {
        this.activityTime = activityTime;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public void setStatus(ActivityStatus status) {
        this.status = status;
    }
}
