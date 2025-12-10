package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for updating an activity.
 * Description is optional but must contain non-whitespace characters when provided.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateActivityRequestDto {

    @Pattern(regexp = ".*\\S.*", message = "description must not be blank")
    private String description;

    /**
     * Default constructor for deserialization.
     */
    public UpdateActivityRequestDto() {
        // Default constructor
    }

    /**
     * Creates a new update activity request.
     *
     * @param description optional activity description
     */
    public UpdateActivityRequestDto(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
