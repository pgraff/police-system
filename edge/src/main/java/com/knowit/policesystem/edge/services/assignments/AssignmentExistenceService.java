package com.knowit.policesystem.edge.services.assignments;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for checking assignment existence in the projection.
 */
@Service
public class AssignmentExistenceService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentExistenceService.class);

    private final ProjectionQueryService projectionQueryService;

    public AssignmentExistenceService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    /**
     * Checks if an assignment exists by assignment ID.
     *
     * @param assignmentId the assignment identifier
     * @return true if the assignment exists, false otherwise
     */
    public boolean exists(String assignmentId) {
        try {
            return projectionQueryService.exists("assignment", assignmentId);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            log.error("Failed to check assignment existence for assignmentId: {}", assignmentId, e);
            // Default to false on error - this will cause 404 which is safer than assuming existence
            return false;
        }
    }
}

