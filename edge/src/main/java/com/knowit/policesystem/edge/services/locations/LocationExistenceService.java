package com.knowit.policesystem.edge.services.locations;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for checking location existence in the projection.
 */
@Service
public class LocationExistenceService {

    private static final Logger log = LoggerFactory.getLogger(LocationExistenceService.class);

    private final ProjectionQueryService projectionQueryService;

    public LocationExistenceService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    /**
     * Checks if a location exists by location ID.
     *
     * @param locationId the location identifier
     * @return true if the location exists, false otherwise
     */
    public boolean exists(String locationId) {
        try {
            return projectionQueryService.exists("location", locationId);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            log.error("Failed to check location existence for locationId: {}", locationId, e);
            // Default to false on error - this will cause 404 which is safer than assuming existence
            return false;
        }
    }
}
