package com.knowit.policesystem.edge.services.activities;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for checking activity existence in the projection.
 */
@Service
public class ActivityExistenceService {

    private static final Logger log = LoggerFactory.getLogger(ActivityExistenceService.class);

    private final ProjectionQueryService projectionQueryService;

    public ActivityExistenceService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    /**
     * Checks if an activity exists by activity ID.
     *
     * @param activityId the activity identifier
     * @return true if the activity exists, false otherwise
     */
    public boolean exists(String activityId) {
        try {
            return projectionQueryService.exists("activity", activityId);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            log.error("Failed to check activity existence for activityId: {}", activityId, e);
            // Default to false on error - this will cause 404 which is safer than assuming existence
            return false;
        }
    }
}

