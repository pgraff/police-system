package com.knowit.policesystem.edge.services.projections;

import com.knowit.policesystem.common.nats.NatsQueryClient;
import com.knowit.policesystem.common.nats.query.ExistsQueryRequest;
import com.knowit.policesystem.common.nats.query.ExistsQueryResponse;
import com.knowit.policesystem.common.nats.query.GetQueryRequest;
import com.knowit.policesystem.common.nats.query.GetQueryResponse;

/**
 * Service for querying projections via NATS request-response.
 * Provides synchronous queries to projection services.
 * 
 * <p>This service routes queries to consolidated projection services based on domain names.
 * The domain names remain unchanged for backward compatibility, and queries are automatically
 * routed to the appropriate projection service via NATS subjects.
 * 
 * <p><b>Domain-to-Projection Mapping:</b>
 * 
 * <p><b>Operational Projection</b> (operational-projection service):
 * <ul>
 *   <li>incident - Incident entities</li>
 *   <li>call - Call entities</li>
 *   <li>dispatch - Dispatch entities</li>
 *   <li>activity - Activity entities</li>
 *   <li>assignment - Assignment entities</li>
 *   <li>involved-party - Involved party role entities</li>
 *   <li>resource-assignment - Resource assignment role entities</li>
 * </ul>
 * 
 * <p><b>Resource Projection</b> (resource-projection service):
 * <ul>
 *   <li>officer - Officer entities</li>
 *   <li>vehicle - Vehicle entities</li>
 *   <li>unit - Unit entities</li>
 *   <li>person - Person entities</li>
 *   <li>location - Location entities</li>
 * </ul>
 * 
 * <p><b>Workforce Projection</b> (workforce-projection service):
 * <ul>
 *   <li>shift - Shift entities</li>
 *   <li>officer-shift - Officer shift role entities</li>
 *   <li>shift-change - Shift change entities</li>
 * </ul>
 * 
 * <p><b>NATS Routing:</b>
 * <p>Queries are sent to NATS subjects following the pattern:
 * <ul>
 *   <li>Existence queries: {@code query.{domain}.exists}</li>
 *   <li>Get queries: {@code query.{domain}.get}</li>
 * </ul>
 * 
 * <p>Each consolidated projection service subscribes to the relevant NATS subjects
 * for its domains. Routing happens automatically - whichever projection service
 * subscribes to the subject will handle the query.
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * // Query for an incident (routes to operational-projection)
 * boolean exists = projectionQueryService.exists("incident", "INC-001");
 * 
 * // Query for an officer (routes to resource-projection)
 * boolean exists = projectionQueryService.exists("officer", "BADGE-001");
 * 
 * // Query for a shift (routes to workforce-projection)
 * boolean exists = projectionQueryService.exists("shift", "SHIFT-001");
 * }</pre>
 */
public class ProjectionQueryService {

    private final NatsQueryClient queryClient;

    public ProjectionQueryService(NatsQueryClient queryClient) {
        this.queryClient = queryClient;
    }

    /**
     * Checks if a resource exists in a projection.
     * 
     * <p>Routes the query to the appropriate projection service based on the domain name.
     * The domain name determines which consolidated projection service handles the query:
     * <ul>
     *   <li>Operational domains (incident, call, dispatch, activity, assignment, involved-party, resource-assignment)
     *       → operational-projection service</li>
     *   <li>Resource domains (officer, vehicle, unit, person, location) → resource-projection service</li>
     *   <li>Workforce domains (shift, officer-shift, shift-change) → workforce-projection service</li>
     * </ul>
     * 
     * <p>The query is sent to NATS subject: {@code query.{domain}.exists}
     *
     * @param domain the domain name (e.g., "officer", "call", "incident", "shift")
     * @param resourceId the resource identifier
     * @return true if the resource exists, false otherwise
     * @throws ProjectionQueryException if the query fails or NATS client is unavailable
     */
    public boolean exists(String domain, String resourceId) throws ProjectionQueryException {
        if (queryClient == null) {
            throw new ProjectionQueryException("NATS query client is not available");
        }
        try {
            // Construct NATS subject: query.{domain}.exists
            // The appropriate projection service subscribes to this subject and handles the query
            String subject = "query." + domain + ".exists";
            ExistsQueryRequest request = new ExistsQueryRequest(null, domain, resourceId);
            ExistsQueryResponse response = queryClient.query(subject, request, ExistsQueryResponse.class);
            return response.isExists();
        } catch (NatsQueryClient.NatsQueryException e) {
            throw new ProjectionQueryException("Failed to query projection for existence: " + e.getMessage(), e);
        }
    }

    /**
     * Gets a resource from a projection.
     * 
     * <p>Routes the query to the appropriate projection service based on the domain name.
     * The domain name determines which consolidated projection service handles the query:
     * <ul>
     *   <li>Operational domains (incident, call, dispatch, activity, assignment, involved-party, resource-assignment)
     *       → operational-projection service</li>
     *   <li>Resource domains (officer, vehicle, unit, person, location) → resource-projection service</li>
     *   <li>Workforce domains (shift, officer-shift, shift-change) → workforce-projection service</li>
     * </ul>
     * 
     * <p>The query is sent to NATS subject: {@code query.{domain}.get}
     * 
     * <p>The returned data is a JSON-serialized projection response object. The exact structure
     * depends on the domain type (e.g., OfficerProjectionResponse, IncidentProjectionResponse, etc.).
     *
     * @param domain the domain name (e.g., "officer", "call", "incident", "shift")
     * @param resourceId the resource identifier
     * @return the resource data as a JSON object (deserialized), or null if not found
     * @throws ProjectionQueryException if the query fails or NATS client is unavailable
     */
    public Object get(String domain, String resourceId) throws ProjectionQueryException {
        if (queryClient == null) {
            throw new ProjectionQueryException("NATS query client is not available");
        }
        try {
            // Construct NATS subject: query.{domain}.get
            // The appropriate projection service subscribes to this subject and handles the query
            String subject = "query." + domain + ".get";
            GetQueryRequest request = new GetQueryRequest(null, domain, resourceId);
            GetQueryResponse response = queryClient.query(subject, request, GetQueryResponse.class);
            return response.getData();
        } catch (NatsQueryClient.NatsQueryException e) {
            throw new ProjectionQueryException("Failed to query projection for resource: " + e.getMessage(), e);
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

