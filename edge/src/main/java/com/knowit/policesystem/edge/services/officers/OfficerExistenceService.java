package com.knowit.policesystem.edge.services.officers;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for checking officer existence in the projection.
 */
@Service
public class OfficerExistenceService {

    private static final Logger log = LoggerFactory.getLogger(OfficerExistenceService.class);

    private final ProjectionQueryService projectionQueryService;

    public OfficerExistenceService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    /**
     * Checks if an officer exists by badge number.
     *
     * @param badgeNumber the officer badge number
     * @return true if the officer exists, false otherwise
     */
    public boolean exists(String badgeNumber) {
        try {
            return projectionQueryService.exists("officer", badgeNumber);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            log.error("Failed to check officer existence for badgeNumber: {}", badgeNumber, e);
            // Default to false on error - this will cause 404 which is safer than assuming existence
            return false;
        }
    }
}

