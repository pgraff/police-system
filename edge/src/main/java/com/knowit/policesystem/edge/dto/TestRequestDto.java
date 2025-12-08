package com.knowit.policesystem.edge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Test DTO for validation testing.
 * Used to verify request validation infrastructure works correctly.
 */
public class TestRequestDto {

    @NotBlank(message = "requiredField is required")
    private String requiredField;

    @Email(message = "emailField must be a valid email address")
    private String emailField;

    @Min(value = 0, message = "numberField must be at least 0")
    @Max(value = 100, message = "numberField must be at most 100")
    private Integer numberField;

    /**
     * Default constructor for Jackson deserialization.
     */
    public TestRequestDto() {
    }

    /**
     * Creates a new test request DTO.
     *
     * @param requiredField the required field
     * @param emailField the email field
     * @param numberField the number field
     */
    public TestRequestDto(String requiredField, String emailField, Integer numberField) {
        this.requiredField = requiredField;
        this.emailField = emailField;
        this.numberField = numberField;
    }

    /**
     * Returns the required field.
     *
     * @return the required field
     */
    public String getRequiredField() {
        return requiredField;
    }

    /**
     * Sets the required field.
     *
     * @param requiredField the required field
     */
    public void setRequiredField(String requiredField) {
        this.requiredField = requiredField;
    }

    /**
     * Returns the email field.
     *
     * @return the email field
     */
    public String getEmailField() {
        return emailField;
    }

    /**
     * Sets the email field.
     *
     * @param emailField the email field
     */
    public void setEmailField(String emailField) {
        this.emailField = emailField;
    }

    /**
     * Returns the number field.
     *
     * @return the number field
     */
    public Integer getNumberField() {
        return numberField;
    }

    /**
     * Sets the number field.
     *
     * @param numberField the number field
     */
    public void setNumberField(Integer numberField) {
        this.numberField = numberField;
    }
}
