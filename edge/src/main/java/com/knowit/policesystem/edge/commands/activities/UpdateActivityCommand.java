package com.knowit.policesystem.edge.commands.activities;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.UpdateActivityRequestDto;

/**
 * Command for updating an activity's details.
 */
public class UpdateActivityCommand extends Command {

    private String activityId;
    private String description;

    /**
     * Default constructor for deserialization.
     */
    public UpdateActivityCommand() {
        super();
    }

    /**
     * Creates a new update activity command from a DTO.
     *
     * @param aggregateId the activity identifier
     * @param dto the request payload
     */
    public UpdateActivityCommand(String aggregateId, UpdateActivityRequestDto dto) {
        super(aggregateId);
        this.activityId = aggregateId;
        this.description = dto.getDescription();
    }

    @Override
    public String getCommandType() {
        return "UpdateActivityCommand";
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
