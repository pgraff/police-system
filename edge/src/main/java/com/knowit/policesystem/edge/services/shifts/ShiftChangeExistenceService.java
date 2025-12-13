package com.knowit.policesystem.edge.services.shifts;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for checking shift change existence in the projection.
 */
@Service
public class ShiftChangeExistenceService {

    private static final Logger log = LoggerFactory.getLogger(ShiftChangeExistenceService.class);

    private final ProjectionQueryService projectionQueryService;

    public ShiftChangeExistenceService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    /**
     * Checks if a shift change exists by shift change ID.
     *
     * @param shiftChangeId the shift change identifier
     * @return true if the shift change exists, false otherwise
     */
    public boolean exists(String shiftChangeId) {
        try {
            return projectionQueryService.exists("shift-change", shiftChangeId);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            log.error("Failed to check shift change existence for shiftChangeId: {}", shiftChangeId, e);
            // Default to false on error - this will cause 404 which is safer than assuming existence
            return false;
        }
    }
}
