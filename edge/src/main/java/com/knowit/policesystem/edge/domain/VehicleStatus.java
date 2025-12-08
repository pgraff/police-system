package com.knowit.policesystem.edge.domain;

/**
 * Status values for vehicles.
 * Matches the VehicleStatus enum in the OpenAPI specification.
 */
public enum VehicleStatus {
    Available,
    Assigned,
    InUse,  // Maps to "In-Use" in OpenAPI spec
    Maintenance,
    OutOfService  // Maps to "Out-of-Service" in OpenAPI spec
}
