package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.knowit.policesystem.edge.domain.ResourceType;
import com.knowit.policesystem.edge.domain.RoleType;
import com.knowit.policesystem.edge.util.FlexibleInstantDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for assigning a resource to an assignment.
 * Matches the AssignResourceRequest schema in the OpenAPI specification.
 */
public class AssignResourceRequestDto {

    @NotBlank(message = "resourceId is required")
    private String resourceId;

    @NotNull(message = "resourceType is required")
    private ResourceType resourceType;

    @NotNull(message = "roleType is required")
    private RoleType roleType;

    @NotBlank(message = "status is required")
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    @JsonDeserialize(using = FlexibleInstantDeserializer.class)
    private Instant startTime;

    /**
     * Default constructor for Jackson deserialization.
     */
    public AssignResourceRequestDto() {
    }

    /**
     * Creates a new assign resource request DTO.
     *
     * @param resourceId the resource identifier (badge number, vehicle unit ID, or unit ID)
     * @param resourceType the type of resource
     * @param roleType the role type
     * @param status the status
     * @param startTime the start time (optional)
     */
    public AssignResourceRequestDto(String resourceId, ResourceType resourceType, RoleType roleType,
                                   String status, Instant startTime) {
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.roleType = roleType;
        this.status = status;
        this.startTime = startTime;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
}
