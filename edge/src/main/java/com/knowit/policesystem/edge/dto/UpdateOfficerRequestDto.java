package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;

import java.time.LocalDate;

/**
 * Request DTO for updating an officer.
 * Matches the UpdateOfficerRequest schema in the OpenAPI specification.
 * All fields are optional for partial updates.
 */
public class UpdateOfficerRequestDto {

    private String firstName;

    private String lastName;

    private String rank;

    @Email(message = "email must be a valid email address")
    private String email;

    private String phoneNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    /**
     * Default constructor for Jackson deserialization.
     */
    public UpdateOfficerRequestDto() {
    }

    /**
     * Creates a new update officer request DTO.
     *
     * @param firstName the first name (optional)
     * @param lastName the last name (optional)
     * @param rank the rank (optional)
     * @param email the email address (optional)
     * @param phoneNumber the phone number (optional)
     * @param hireDate the hire date (optional)
     */
    public UpdateOfficerRequestDto(String firstName, String lastName, String rank,
                                   String email, String phoneNumber, LocalDate hireDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.rank = rank;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.hireDate = hireDate;
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

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }
}
