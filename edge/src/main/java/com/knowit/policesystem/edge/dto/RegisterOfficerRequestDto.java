package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.knowit.policesystem.edge.domain.OfficerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request DTO for registering an officer.
 * Matches the RegisterOfficerRequest schema in the OpenAPI specification.
 */
public class RegisterOfficerRequestDto {

    @NotBlank(message = "badgeNumber is required")
    private String badgeNumber;

    @NotBlank(message = "firstName is required")
    private String firstName;

    @NotBlank(message = "lastName is required")
    private String lastName;

    @NotBlank(message = "rank is required")
    private String rank;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    private String phoneNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    @NotNull(message = "status is required")
    private OfficerStatus status;

    /**
     * Default constructor for Jackson deserialization.
     */
    public RegisterOfficerRequestDto() {
    }

    /**
     * Creates a new register officer request DTO.
     *
     * @param badgeNumber the badge number
     * @param firstName the first name
     * @param lastName the last name
     * @param rank the rank
     * @param email the email address
     * @param phoneNumber the phone number (optional)
     * @param hireDate the hire date (optional)
     * @param status the officer status
     */
    public RegisterOfficerRequestDto(String badgeNumber, String firstName, String lastName,
                                    String rank, String email, String phoneNumber,
                                    LocalDate hireDate, OfficerStatus status) {
        this.badgeNumber = badgeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.rank = rank;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.hireDate = hireDate;
        this.status = status;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
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

    public OfficerStatus getStatus() {
        return status;
    }

    public void setStatus(OfficerStatus status) {
        this.status = status;
    }
}
