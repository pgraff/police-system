package com.knowit.policesystem.edge.commands.vehicles;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.VehicleStatus;
import com.knowit.policesystem.edge.domain.VehicleType;
import com.knowit.policesystem.edge.dto.RegisterVehicleRequestDto;

import java.time.LocalDate;

/**
 * Command for registering a vehicle.
 * This command is processed by RegisterVehicleCommandHandler.
 */
public class RegisterVehicleCommand extends Command {

    private String unitId;
    private VehicleType vehicleType;
    private String licensePlate;
    private String vin;
    private VehicleStatus status;
    private LocalDate lastMaintenanceDate;

    /**
     * Default constructor for deserialization.
     */
    public RegisterVehicleCommand() {
        super();
    }

    /**
     * Creates a new register vehicle command from a DTO.
     *
     * @param aggregateId the aggregate identifier (unitId)
     * @param dto the request DTO containing vehicle data
     */
    public RegisterVehicleCommand(String aggregateId, RegisterVehicleRequestDto dto) {
        super(aggregateId);
        this.unitId = dto.getUnitId();
        this.vehicleType = dto.getVehicleType();
        this.licensePlate = dto.getLicensePlate();
        this.vin = dto.getVin();
        this.status = dto.getStatus();
        this.lastMaintenanceDate = dto.getLastMaintenanceDate();
    }

    @Override
    public String getCommandType() {
        return "RegisterVehicleCommand";
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
