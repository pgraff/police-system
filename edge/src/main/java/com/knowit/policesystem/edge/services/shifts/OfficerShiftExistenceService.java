package com.knowit.policesystem.edge.services.shifts;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for checking officer shift existence in the projection.
 */
@Service
public class OfficerShiftExistenceService {

    private static final Logger log = LoggerFactory.getLogger(OfficerShiftExistenceService.class);

    private final ProjectionQueryService projectionQueryService;

    public OfficerShiftExistenceService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    /**
     * Checks if an officer shift exists by officer shift ID.
     *
     * @param id the officer shift identifier (Long ID)
     * @return true if the officer shift exists, false otherwise
     */
    public boolean exists(Long id) {
        try {
            return projectionQueryService.exists("officer-shift", String.valueOf(id));
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            log.error("Failed to check officer shift existence for id: {}", id, e);
            // Default to false on error - this will cause 404 which is safer than assuming existence
            return false;
        }
    }
}
