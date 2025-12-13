package com.knowit.policesystem.edge.services.persons;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for checking person existence in the projection.
 */
@Service
public class PersonExistenceService {

    private static final Logger log = LoggerFactory.getLogger(PersonExistenceService.class);

    private final ProjectionQueryService projectionQueryService;

    public PersonExistenceService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    /**
     * Checks if a person exists by person ID.
     *
     * @param personId the person identifier
     * @return true if the person exists, false otherwise
     */
    public boolean exists(String personId) {
        try {
            return projectionQueryService.exists("person", personId);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            log.error("Failed to check person existence for personId: {}", personId, e);
            // Default to false on error - this will cause 404 which is safer than assuming existence
            return false;
        }
    }
}
