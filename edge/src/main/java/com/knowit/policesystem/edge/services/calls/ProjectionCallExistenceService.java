package com.knowit.policesystem.edge.services.calls;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Service for checking call existence in the projection via NATS queries.
 * 
 * This service is conditionally created - it will not be created if a test provides
 * a bean named "callExistenceService" (which test configurations do).
 * 
 * Note: @Primary is removed to allow test configurations to override with their own @Primary bean.
 */
@Service
@ConditionalOnMissingBean(name = "callExistenceService")
public class ProjectionCallExistenceService implements CallExistenceService {

    private static final Logger log = LoggerFactory.getLogger(ProjectionCallExistenceService.class);

    private final ProjectionQueryService projectionQueryService;

    public ProjectionCallExistenceService(ProjectionQueryService projectionQueryService) {
        this.projectionQueryService = projectionQueryService;
    }

    @Override
    public boolean exists(String callId) {
        try {
            return projectionQueryService.exists("call", callId);
        } catch (ProjectionQueryService.ProjectionQueryException e) {
            log.error("Failed to check call existence for callId: {}", callId, e);
            // Default to false on error - this will cause 404 which is safer than assuming existence
            return false;
        }
    }
}

