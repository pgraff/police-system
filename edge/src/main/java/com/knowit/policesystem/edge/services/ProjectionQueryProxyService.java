package com.knowit.policesystem.edge.services;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for proxying queries to projection services.
 * Provides a unified interface for querying projections with caching and aggregation support.
 */
@Service
public class ProjectionQueryProxyService {

    private final ProjectionQueryService projectionQueryService;

    public ProjectionQueryProxyService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    /**
     * Gets a full resource with related data from projections.
     * Aggregates data from multiple projections if needed.
     *
     * @param domain the domain name
     * @param resourceId the resource identifier
     * @return the resource data
     */
    public Object getFullResource(String domain, String resourceId) {
        try {
            return projectionQueryService.get(domain, resourceId);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            throw new RuntimeException("Failed to query projection: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a resource exists.
     *
     * @param domain the domain name
     * @param resourceId the resource identifier
     * @return true if exists, false otherwise
     */
    public boolean exists(String domain, String resourceId) {
        try {
            return projectionQueryService.exists(domain, resourceId);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            throw new RuntimeException("Failed to check existence: " + e.getMessage(), e);
        }
    }
}
