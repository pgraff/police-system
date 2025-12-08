package com.knowit.policesystem.edge.commands.units;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.UnitStatus;
import com.knowit.policesystem.edge.domain.UnitType;
import com.knowit.policesystem.edge.dto.CreateUnitRequestDto;

/**
 * Command for creating a unit.
 * This command is processed by CreateUnitCommandHandler.
 */
public class CreateUnitCommand extends Command {

    private String unitId;
    private UnitType unitType;
    private UnitStatus status;

    /**
     * Default constructor for deserialization.
     */
    public CreateUnitCommand() {
        super();
    }

    /**
     * Creates a new create unit command from a DTO.
     *
     * @param aggregateId the aggregate identifier (unitId)
     * @param dto the request DTO containing unit data
     */
    public CreateUnitCommand(String aggregateId, CreateUnitRequestDto dto) {
        super(aggregateId);
        this.unitId = dto.getUnitId();
        this.unitType = dto.getUnitType();
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "CreateUnitCommand";
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
