package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.ShiftRoleType;
import com.knowit.policesystem.edge.dto.UpdateOfficerShiftRequestDto;

/**
 * Command for updating an officer shift.
 * Processed by UpdateOfficerShiftCommandHandler.
 */
public class UpdateOfficerShiftCommand extends Command {

    private String shiftId;
    private String badgeNumber;
    private ShiftRoleType shiftRoleType;

    /**
     * Default constructor for deserialization.
     */
    public UpdateOfficerShiftCommand() {
        super();
    }

    /**
     * Creates a new update officer shift command from the request DTO.
     *
     * @param aggregateId the aggregate identifier (shiftId from path)
     * @param badgeNumber the badge number from path
     * @param dto the request DTO containing shift role type
     */
    public UpdateOfficerShiftCommand(String aggregateId, String badgeNumber, UpdateOfficerShiftRequestDto dto) {
        super(aggregateId);
        this.shiftId = aggregateId;
        this.badgeNumber = badgeNumber;
        this.shiftRoleType = dto != null ? dto.getShiftRoleType() : null;
    }

    @Override
    public String getCommandType() {
        return "UpdateOfficerShiftCommand";
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public ShiftRoleType getShiftRoleType() {
        return shiftRoleType;
    }

    public void setShiftRoleType(ShiftRoleType shiftRoleType) {
        this.shiftRoleType = shiftRoleType;
    }
}
