package com.knowit.policesystem.edge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for batch creating officers.
 * Accepts an array of officer registration requests.
 */
public class BatchCreateOfficersRequestDto {

    @NotEmpty(message = "officers list cannot be empty")
    @Valid
    private List<RegisterOfficerRequestDto> officers;

    /**
     * Default constructor for Jackson deserialization.
     */
    public BatchCreateOfficersRequestDto() {
    }

    /**
     * Creates a new batch create officers request DTO.
     *
     * @param officers the list of officer registration requests
     */
    public BatchCreateOfficersRequestDto(List<RegisterOfficerRequestDto> officers) {
        this.officers = officers;
    }

    public List<RegisterOfficerRequestDto> getOfficers() {
        return officers;
    }

    public void setOfficers(List<RegisterOfficerRequestDto> officers) {
        this.officers = officers;
    }
}
