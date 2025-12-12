package com.knowit.policesystem.edge.services.projections;

import com.knowit.policesystem.common.nats.NatsQueryClient;
import com.knowit.policesystem.common.nats.query.ExistsQueryRequest;
import com.knowit.policesystem.common.nats.query.ExistsQueryResponse;

/**
 * Service for querying projections via NATS request-response.
 * Provides synchronous queries to projection services.
 */
public class ProjectionQueryService {

    private final NatsQueryClient queryClient;

    public ProjectionQueryService(NatsQueryClient queryClient) {
        this.queryClient = queryClient;
    }

    /**
     * Checks if a resource exists in a projection.
     *
     * @param domain the domain name (e.g., "officer", "call", "incident")
     * @param resourceId the resource identifier
     * @return true if the resource exists, false otherwise
     * @throws ProjectionQueryException if the query fails
     */
    public boolean exists(String domain, String resourceId) throws ProjectionQueryException {
        if (queryClient == null) {
            throw new ProjectionQueryException("NATS query client is not available");
        }
        try {
            String subject = "query." + domain + ".exists";
            ExistsQueryRequest request = new ExistsQueryRequest(null, domain, resourceId);
            ExistsQueryResponse response = queryClient.query(subject, request, ExistsQueryResponse.class);
            return response.isExists();
        } catch (NatsQueryClient.NatsQueryException e) {
            throw new ProjectionQueryException("Failed to query projection for existence: " + e.getMessage(), e);
        }
    }

    /**
     * Exception thrown when a projection query fails.
     */
    public static class ProjectionQueryException extends Exception {
        public ProjectionQueryException(String message) {
            super(message);
        }

        public ProjectionQueryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

