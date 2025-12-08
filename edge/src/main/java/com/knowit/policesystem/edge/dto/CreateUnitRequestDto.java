package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.domain.UnitStatus;
import com.knowit.policesystem.edge.domain.UnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a unit.
 * Matches the CreateUnitRequest schema in the OpenAPI specification.
 */
public class CreateUnitRequestDto {

    @NotBlank(message = "unitId is required")
    private String unitId;

    @NotNull(message = "unitType is required")
    private UnitType unitType;

    @NotNull(message = "status is required")
    private UnitStatus status;

    /**
     * Default constructor for Jackson deserialization.
     */
    public CreateUnitRequestDto() {
    }

    /**
     * Creates a new create unit request DTO.
     *
     * @param unitId the unit ID
     * @param unitType the unit type
     * @param status the unit status
     */
    public CreateUnitRequestDto(String unitId, UnitType unitType, UnitStatus status) {
        this.unitId = unitId;
        this.unitType = unitType;
        this.status = status;
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
