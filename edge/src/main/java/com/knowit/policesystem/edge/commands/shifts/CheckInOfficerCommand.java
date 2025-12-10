package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.ShiftRoleType;
import com.knowit.policesystem.edge.dto.CheckInOfficerRequestDto;

import java.time.Instant;

/**
 * Command for checking in an officer to a shift.
 * Processed by CheckInOfficerCommandHandler.
 */
public class CheckInOfficerCommand extends Command {

    private String shiftId;
    private String badgeNumber;
    private Instant checkInTime;
    private ShiftRoleType shiftRoleType;

    /**
     * Default constructor for deserialization.
     */
    public CheckInOfficerCommand() {
        super();
    }

    /**
     * Creates a new check-in officer command from the request DTO.
     *
     * @param aggregateId the aggregate identifier (shiftId from path)
     * @param badgeNumber the badge number from path
     * @param dto the request DTO containing check-in time and shift role type
     */
    public CheckInOfficerCommand(String aggregateId, String badgeNumber, CheckInOfficerRequestDto dto) {
        super(aggregateId);
        this.shiftId = aggregateId;
        this.badgeNumber = badgeNumber;
        this.checkInTime = dto != null ? dto.getCheckInTime() : null;
        this.shiftRoleType = dto != null ? dto.getShiftRoleType() : null;
    }

    @Override
    public String getCommandType() {
        return "CheckInOfficerCommand";
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

    public Instant getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(Instant checkInTime) {
        this.checkInTime = checkInTime;
    }

    public ShiftRoleType getShiftRoleType() {
        return shiftRoleType;
    }

    public void setShiftRoleType(ShiftRoleType shiftRoleType) {
        this.shiftRoleType = shiftRoleType;
    }
}
