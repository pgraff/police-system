package com.knowit.policesystem.edge.domain;

/**
 * Status values for dispatches.
 * Matches the DispatchStatus enum in the OpenAPI specification.
 */
public enum DispatchStatus {
    Created,
    Sent,
    Acknowledged,
    Completed,
    Cancelled
}
