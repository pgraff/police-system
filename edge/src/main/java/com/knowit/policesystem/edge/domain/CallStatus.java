package com.knowit.policesystem.edge.domain;

/**
 * Status values for calls.
 * Matches the CallStatus enum in the OpenAPI specification.
 */
public enum CallStatus {
    Received,
    Dispatched,
    Arrived,
    InProgress,
    Cleared,
    Closed
}
