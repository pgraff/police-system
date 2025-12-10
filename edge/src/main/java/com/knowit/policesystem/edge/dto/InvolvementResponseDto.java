package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for involvement operations.
 * Matches the InvolvementResponse schema in the OpenAPI specification.
 */
public class InvolvementResponseDto {

    private String involvementId;

    /**
     * Default constructor for Jackson serialization.
     */
    public InvolvementResponseDto() {
    }

    /**
     * Creates a new involvement response DTO.
     *
     * @param involvementId the involvement identifier
     */
    public InvolvementResponseDto(String involvementId) {
        this.involvementId = involvementId;
    }

    public String getInvolvementId() {
        return involvementId;
    }

    public void setInvolvementId(String involvementId) {
        this.involvementId = involvementId;
    }
}
