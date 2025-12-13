package com.knowit.policesystem.edge.services.units;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for checking unit existence in the projection.
 */
@Service
public class UnitExistenceService {

    private static final Logger log = LoggerFactory.getLogger(UnitExistenceService.class);

    private final ProjectionQueryService projectionQueryService;

    public UnitExistenceService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    /**
     * Checks if a unit exists by unit ID.
     *
     * @param unitId the unit identifier
     * @return true if the unit exists, false otherwise
     */
    public boolean exists(String unitId) {
        try {
            return projectionQueryService.exists("unit", unitId);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            log.error("Failed to check unit existence for unitId: {}", unitId, e);
            // Default to false on error - this will cause 404 which is safer than assuming existence
            return false;
        }
    }
}
