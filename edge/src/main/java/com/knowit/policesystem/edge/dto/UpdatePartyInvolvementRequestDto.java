package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.knowit.policesystem.edge.domain.PartyRoleType;
import jakarta.validation.constraints.AssertTrue;

/**
 * Request DTO for updating a party involvement.
 * All fields are optional, but at least one must be provided.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdatePartyInvolvementRequestDto {

    private PartyRoleType partyRoleType;

    private String description;

    /**
     * Default constructor for Jackson deserialization.
     */
    public UpdatePartyInvolvementRequestDto() {
    }

    /**
     * Creates a new update party involvement request DTO.
     *
     * @param partyRoleType the party role type (optional)
     * @param description the description (optional)
     */
    public UpdatePartyInvolvementRequestDto(PartyRoleType partyRoleType, String description) {
        this.partyRoleType = partyRoleType;
        this.description = description;
    }

    public PartyRoleType getPartyRoleType() {
        return partyRoleType;
    }

    public void setPartyRoleType(PartyRoleType partyRoleType) {
        this.partyRoleType = partyRoleType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @AssertTrue(message = "At least one of partyRoleType or description must be provided")
    public boolean isAtLeastOneFieldProvided() {
        return partyRoleType != null
                || (description != null && !description.trim().isEmpty());
    }
}
