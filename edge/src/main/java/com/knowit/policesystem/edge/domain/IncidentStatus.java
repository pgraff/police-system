package com.knowit.policesystem.edge.domain;

/**
 * Status values for incidents.
 * Matches the IncidentStatus enum in the OpenAPI specification.
 */
public enum IncidentStatus {
    Reported,
    Dispatched,
    Arrived,
    InProgress,
    Cleared,
    Closed
}
