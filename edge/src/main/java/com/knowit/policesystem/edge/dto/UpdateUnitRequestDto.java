package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.domain.UnitStatus;
import com.knowit.policesystem.edge.domain.UnitType;

/**
 * Request DTO for updating a unit.
 * Matches the UpdateUnitRequest schema in the OpenAPI specification.
 * All fields are optional for partial updates.
 */
public class UpdateUnitRequestDto {

    private UnitType unitType;

    private UnitStatus status;

    /**
     * Default constructor for Jackson deserialization.
     */
    public UpdateUnitRequestDto() {
    }

    /**
     * Creates a new update unit request DTO.
     *
     * @param unitType the unit type (optional)
     * @param status the unit status (optional)
     */
    public UpdateUnitRequestDto(UnitType unitType, UnitStatus status) {
        this.unitType = unitType;
        this.status = status;
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
