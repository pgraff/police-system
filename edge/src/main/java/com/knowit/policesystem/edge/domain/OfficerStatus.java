package com.knowit.policesystem.edge.domain;

/**
 * Status values for officers.
 * Matches the OfficerStatus enum in the OpenAPI specification.
 */
public enum OfficerStatus {
    Active,
    OnDuty,
    OffDuty,
    Suspended,
    Retired
}
