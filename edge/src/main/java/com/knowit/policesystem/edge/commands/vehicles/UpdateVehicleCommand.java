package com.knowit.policesystem.edge.commands.vehicles;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.VehicleStatus;
import com.knowit.policesystem.edge.domain.VehicleType;
import com.knowit.policesystem.edge.dto.UpdateVehicleRequestDto;

import java.time.LocalDate;

/**
 * Command for updating a vehicle.
 * This command is processed by UpdateVehicleCommandHandler.
 */
public class UpdateVehicleCommand extends Command {

    private String unitId;
    private VehicleType vehicleType;
    private String licensePlate;
    private String vin;
    private VehicleStatus status;
    private LocalDate lastMaintenanceDate;

    /**
     * Default constructor for deserialization.
     */
    public UpdateVehicleCommand() {
        super();
    }

    /**
     * Creates a new update vehicle command from a DTO.
     *
     * @param aggregateId the aggregate identifier (unitId)
     * @param dto the request DTO containing vehicle update data
     */
    public UpdateVehicleCommand(String aggregateId, UpdateVehicleRequestDto dto) {
        super(aggregateId);
        this.unitId = aggregateId;
        this.vehicleType = dto.getVehicleType();
        this.licensePlate = dto.getLicensePlate();
        this.vin = dto.getVin();
        this.status = dto.getStatus();
        this.lastMaintenanceDate = dto.getLastMaintenanceDate();
    }

    @Override
    public String getCommandType() {
        return "UpdateVehicleCommand";
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public LocalDate getLastMaintenanceDate() {
        return lastMaintenanceDate;
    }

    public void setLastMaintenanceDate(LocalDate lastMaintenanceDate) {
        this.lastMaintenanceDate = lastMaintenanceDate;
    }
}
