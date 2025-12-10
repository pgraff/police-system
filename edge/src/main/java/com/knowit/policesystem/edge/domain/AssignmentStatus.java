package com.knowit.policesystem.edge.domain;

/**
 * Status values for assignments.
 * Matches the AssignmentStatus enum in the OpenAPI specification.
 * Note: InProgress maps to "In-Progress" in the API/events.
 */
public enum AssignmentStatus {
    Created,
    Assigned,
    InProgress,
    Completed,
    Cancelled
}
