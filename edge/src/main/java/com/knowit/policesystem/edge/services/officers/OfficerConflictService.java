package com.knowit.policesystem.edge.services.officers;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for checking officer conflicts (e.g., duplicate badge numbers).
 */
@Service
public class OfficerConflictService {

    private static final Logger log = LoggerFactory.getLogger(OfficerConflictService.class);

    private final ProjectionQueryService projectionQueryService;

    public OfficerConflictService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    /**
     * Checks if an officer with the given badge number already exists.
     *
     * @param badgeNumber the badge number to check
     * @return true if an officer with this badge number exists, false otherwise
     */
    public boolean badgeNumberExists(String badgeNumber) {
        try {
            return projectionQueryService.exists("officer", badgeNumber);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            log.error("Failed to check badge number existence for badgeNumber: {}", badgeNumber, e);
            // Default to false on error - this allows creation to proceed
            // In a production system, you might want to fail safe and return true
            return false;
        }
    }
}

