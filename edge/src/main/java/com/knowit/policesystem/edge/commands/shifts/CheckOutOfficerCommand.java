package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.CheckOutOfficerRequestDto;

import java.time.Instant;

/**
 * Command for checking out an officer from a shift.
 * Processed by CheckOutOfficerCommandHandler.
 */
public class CheckOutOfficerCommand extends Command {

    private String shiftId;
    private String badgeNumber;
    private Instant checkOutTime;

    /**
     * Default constructor for deserialization.
     */
    public CheckOutOfficerCommand() {
        super();
    }

    /**
     * Creates a new check-out officer command from the request DTO.
     *
     * @param aggregateId the aggregate identifier (shiftId from path)
     * @param badgeNumber the badge number from path
     * @param dto the request DTO containing check-out time
     */
    public CheckOutOfficerCommand(String aggregateId, String badgeNumber, CheckOutOfficerRequestDto dto) {
        super(aggregateId);
        this.shiftId = aggregateId;
        this.badgeNumber = badgeNumber;
        this.checkOutTime = dto != null ? dto.getCheckOutTime() : null;
    }

    @Override
    public String getCommandType() {
        return "CheckOutOfficerCommand";
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

    public Instant getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(Instant checkOutTime) {
        this.checkOutTime = checkOutTime;
    }
}
