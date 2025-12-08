package com.knowit.policesystem.edge.dto;

/**
 * Response DTO for person operations.
 */
public class PersonResponseDto {

    private String personId;

    /**
     * Default constructor for Jackson serialization.
     */
    public PersonResponseDto() {
    }

    /**
     * Creates a new person response DTO.
     *
     * @param personId the person ID
     */
    public PersonResponseDto(String personId) {
        this.personId = personId;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }
}
