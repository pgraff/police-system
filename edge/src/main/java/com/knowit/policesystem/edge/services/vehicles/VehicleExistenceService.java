package com.knowit.policesystem.edge.services.vehicles;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for checking vehicle existence in the projection.
 */
@Service
public class VehicleExistenceService {

    private static final Logger log = LoggerFactory.getLogger(VehicleExistenceService.class);

    private final ProjectionQueryService projectionQueryService;

    public VehicleExistenceService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    /**
     * Checks if a vehicle exists by unit ID.
     *
     * @param unitId the vehicle unit identifier
     * @return true if the vehicle exists, false otherwise
     */
    public boolean exists(String unitId) {
        try {
            return projectionQueryService.exists("vehicle", unitId);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            log.error("Failed to check vehicle existence for unitId: {}", unitId, e);
            // Default to false on error - this will cause 404 which is safer than assuming existence
            return false;
        }
    }
}
