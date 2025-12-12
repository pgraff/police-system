package com.knowit.policesystem.edge.services.incidents;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for checking incident existence in the projection.
 */
@Service
public class IncidentExistenceService {

    private static final Logger log = LoggerFactory.getLogger(IncidentExistenceService.class);

    private final ProjectionQueryService projectionQueryService;

    public IncidentExistenceService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    /**
     * Checks if an incident exists by incident ID.
     *
     * @param incidentId the incident identifier
     * @return true if the incident exists, false otherwise
     */
    public boolean exists(String incidentId) {
        try {
            return projectionQueryService.exists("incident", incidentId);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            log.error("Failed to check incident existence for incidentId: {}", incidentId, e);
            // Default to false on error - this will cause 404 which is safer than assuming existence
            return false;
        }
    }
}

