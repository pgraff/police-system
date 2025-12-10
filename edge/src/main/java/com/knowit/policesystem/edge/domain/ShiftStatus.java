package com.knowit.policesystem.edge.domain;

/**
 * Status values for shifts.
 * Matches the ShiftStatus enum in the OpenAPI specification.
 * Note: InProgress maps to "In-Progress" when serialized for events.
 */
public enum ShiftStatus {
    Started,
    InProgress,
    Ended,
    Cancelled
}
