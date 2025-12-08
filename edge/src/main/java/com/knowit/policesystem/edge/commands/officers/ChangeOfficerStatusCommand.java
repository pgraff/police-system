package com.knowit.policesystem.edge.commands.officers;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.ChangeOfficerStatusRequestDto;

/**
 * Command for changing an officer's status.
 * This command is processed by ChangeOfficerStatusCommandHandler.
 */
public class ChangeOfficerStatusCommand extends Command {

    private String badgeNumber;
    private String status;

    /**
     * Default constructor for deserialization.
     */
    public ChangeOfficerStatusCommand() {
        super();
    }

    /**
     * Creates a new change officer status command from a DTO.
     *
     * @param aggregateId the aggregate identifier (badgeNumber)
     * @param dto the request DTO containing status change data
     */
    public ChangeOfficerStatusCommand(String aggregateId, ChangeOfficerStatusRequestDto dto) {
        super(aggregateId);
        this.badgeNumber = aggregateId;
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "ChangeOfficerStatusCommand";
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
