package com.knowit.policesystem.edge.commands.vehicles;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.ChangeVehicleStatusRequestDto;

/**
 * Command for changing a vehicle's status.
 * This command is processed by ChangeVehicleStatusCommandHandler.
 */
public class ChangeVehicleStatusCommand extends Command {

    private String unitId;
    private String status;

    /**
     * Default constructor for deserialization.
     */
    public ChangeVehicleStatusCommand() {
        super();
    }

    /**
     * Creates a new change vehicle status command from a DTO.
     *
     * @param aggregateId the aggregate identifier (unitId)
     * @param dto the request DTO containing status change data
     */
    public ChangeVehicleStatusCommand(String aggregateId, ChangeVehicleStatusRequestDto dto) {
        super(aggregateId);
        this.unitId = aggregateId;
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "ChangeVehicleStatusCommand";
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
