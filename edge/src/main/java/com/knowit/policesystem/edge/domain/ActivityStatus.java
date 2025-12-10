package com.knowit.policesystem.edge.domain;

/**
 * Status values for activities.
 * Matches the ActivityStatus enum in the OpenAPI specification.
 * Note: InProgress maps to "In-Progress" in the API/events.
 */
public enum ActivityStatus {
    Started,
    InProgress,
    Completed,
    Cancelled
}
