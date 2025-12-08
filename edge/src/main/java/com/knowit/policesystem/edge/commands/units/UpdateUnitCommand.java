package com.knowit.policesystem.edge.commands.units;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.UnitStatus;
import com.knowit.policesystem.edge.domain.UnitType;
import com.knowit.policesystem.edge.dto.UpdateUnitRequestDto;

/**
 * Command for updating a unit.
 * This command is processed by UpdateUnitCommandHandler.
 */
public class UpdateUnitCommand extends Command {

    private String unitId;
    private UnitType unitType;
    private UnitStatus status;

    /**
     * Default constructor for deserialization.
     */
    public UpdateUnitCommand() {
        super();
    }

    /**
     * Creates a new update unit command from a DTO.
     *
     * @param aggregateId the aggregate identifier (unitId)
     * @param dto the request DTO containing unit update data
     */
    public UpdateUnitCommand(String aggregateId, UpdateUnitRequestDto dto) {
        super(aggregateId);
        this.unitId = aggregateId;
        this.unitType = dto.getUnitType();
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "UpdateUnitCommand";
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public UnitType getUnitType() {
        return unitType;
    }

    public void setUnitType(UnitType unitType) {
        this.unitType = unitType;
    }

    public UnitStatus getStatus() {
        return status;
    }

    public void setStatus(UnitStatus status) {
        this.status = status;
    }
}
