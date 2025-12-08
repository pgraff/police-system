package com.knowit.policesystem.edge.domain;

/**
 * Status values for units.
 * Matches the UnitStatus enum in the OpenAPI specification.
 */
public enum UnitStatus {
    Available,
    Assigned,
    InUse,  // Maps to "In-Use" in OpenAPI spec
    Maintenance,
    OutOfService  // Maps to "Out-of-Service" in OpenAPI spec
}
