package com.knowit.policesystem.edge.domain;

/**
 * Status values for resource assignments.
 * Matches the ResourceAssignmentStatus enum in the OpenAPI specification.
 * Note: InProgress maps to "In-Progress" in the API/events.
 */
public enum ResourceAssignmentStatus {
    Assigned,
    InProgress,
    Completed,
    Cancelled
}
