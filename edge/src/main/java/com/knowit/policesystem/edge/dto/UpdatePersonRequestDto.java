package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.knowit.policesystem.edge.domain.Gender;
import com.knowit.policesystem.edge.domain.Race;

import java.time.LocalDate;

/**
 * Request DTO for updating a person.
 * Matches the UpdatePersonRequest schema in the OpenAPI specification.
 * All fields are optional for partial updates.
 */
public class UpdatePersonRequestDto {

    private String firstName;

    private String lastName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private Gender gender;

    private Race race;

    private String phoneNumber;

    /**
     * Default constructor for Jackson deserialization.
     */
    public UpdatePersonRequestDto() {
    }

    /**
     * Creates a new update person request DTO.
     *
     * @param firstName the first name (optional)
     * @param lastName the last name (optional)
     * @param dateOfBirth the date of birth (optional)
     * @param gender the gender (optional)
     * @param race the race (optional)
     * @param phoneNumber the phone number (optional)
     */
    public UpdatePersonRequestDto(String firstName, String lastName, LocalDate dateOfBirth,
                                  Gender gender, Race race, String phoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.race = race;
        this.phoneNumber = phoneNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Race getRace() {
        return race;
    }

    public void setRace(Race race) {
        this.race = race;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
