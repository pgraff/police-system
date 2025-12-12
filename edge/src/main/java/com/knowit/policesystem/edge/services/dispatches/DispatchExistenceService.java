package com.knowit.policesystem.edge.services.dispatches;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for checking dispatch existence in the projection.
 */
@Service
public class DispatchExistenceService {

    private static final Logger log = LoggerFactory.getLogger(DispatchExistenceService.class);

    private final ProjectionQueryService projectionQueryService;

    public DispatchExistenceService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    /**
     * Checks if a dispatch exists by dispatch ID.
     *
     * @param dispatchId the dispatch identifier
     * @return true if the dispatch exists, false otherwise
     */
    public boolean exists(String dispatchId) {
        try {
            return projectionQueryService.exists("dispatch", dispatchId);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            log.error("Failed to check dispatch existence for dispatchId: {}", dispatchId, e);
            // Default to false on error - this will cause 404 which is safer than assuming existence
            return false;
        }
    }
}

