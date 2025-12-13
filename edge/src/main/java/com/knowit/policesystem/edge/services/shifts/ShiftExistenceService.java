package com.knowit.policesystem.edge.services.shifts;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for checking shift existence in the projection.
 */
@Service
public class ShiftExistenceService {

    private static final Logger log = LoggerFactory.getLogger(ShiftExistenceService.class);

    private final ProjectionQueryService projectionQueryService;

    public ShiftExistenceService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    /**
     * Checks if a shift exists by shift ID.
     *
     * @param shiftId the shift identifier
     * @return true if the shift exists, false otherwise
     */
    public boolean exists(String shiftId) {
        try {
            return projectionQueryService.exists("shift", shiftId);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            log.error("Failed to check shift existence for shiftId: {}", shiftId, e);
            // Default to false on error - this will cause 404 which is safer than assuming existence
            return false;
        }
    }
}
