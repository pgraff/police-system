package com.knowit.policesystem.projection.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.nats.query.ExistsQueryRequest;
import com.knowit.policesystem.common.nats.query.ExistsQueryResponse;
import com.knowit.policesystem.common.nats.query.GetQueryRequest;
import com.knowit.policesystem.common.nats.query.GetQueryResponse;
import com.knowit.policesystem.projection.api.ActivityProjectionResponse;
import com.knowit.policesystem.projection.api.AssignmentProjectionResponse;
import com.knowit.policesystem.projection.api.CallProjectionResponse;
import com.knowit.policesystem.projection.api.DispatchProjectionResponse;
import com.knowit.policesystem.projection.api.IncidentProjectionResponse;
import com.knowit.policesystem.projection.api.InvolvedPartyProjectionResponse;
import com.knowit.policesystem.projection.api.ResourceAssignmentProjectionResponse;
import com.knowit.policesystem.projection.config.NatsProperties;
import com.knowit.policesystem.projection.service.OperationalProjectionService;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.Nats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * NATS query handler for operational projection.
 * Handles synchronous query requests from the edge service for all operational entities.
 */
@Component
public class OperationalNatsQueryHandler {

    private static final Logger log = LoggerFactory.getLogger(OperationalNatsQueryHandler.class);

    private final NatsProperties properties;
    private final OperationalProjectionService projectionService;
    private final ObjectMapper objectMapper;

    private Connection connection;
    private Dispatcher dispatcher;
    private List<String> querySubjects;

    public OperationalNatsQueryHandler(NatsProperties properties,
                                       OperationalProjectionService projectionService,
                                       ObjectMapper objectMapper) {
        this.properties = properties;
        this.projectionService = projectionService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void start() {
        if (!properties.isEnabled() || !properties.isQueryEnabled()) {
            log.info("NATS query handler disabled");
            return;
        }
        try {
            connection = Nats.connect(properties.getUrl());
            dispatcher = connection.createDispatcher(this::handleQuery);

            String prefix = properties.getQuerySubjectPrefix().isBlank()
                    ? "query"
                    : properties.getQuerySubjectPrefix();

            querySubjects = new ArrayList<>();
            querySubjects.add(prefix + ".incident.>");
            querySubjects.add(prefix + ".call.>");
            querySubjects.add(prefix + ".dispatch.>");
            querySubjects.add(prefix + ".activity.>");
            querySubjects.add(prefix + ".assignment.>");
            querySubjects.add(prefix + ".involved-party.>");
            querySubjects.add(prefix + ".resource-assignment.>");

            querySubjects.forEach(subject -> {
                dispatcher.subscribe(subject);
                log.info("Subscribed to NATS query subject {}", subject);
            });
        } catch (Exception e) {
            log.error("Failed to start NATS query handler", e);
        }
    }

    private void handleQuery(Message message) {
        try {
            String subject = message.getSubject();
            String payload = new String(message.getData(), StandardCharsets.UTF_8);

            log.debug("Received query request on subject {}: {}", subject, payload);

            if (subject.endsWith(".exists")) {
                handleExistsQuery(message, payload, subject);
            } else if (subject.endsWith(".get")) {
                handleGetQuery(message, payload, subject);
            } else {
                log.warn("Unknown query operation for subject: {}", subject);
                sendErrorResponse(message, "Unknown query operation: " + subject);
            }
        } catch (Exception e) {
            log.error("Failed to handle query request", e);
            sendErrorResponse(message, "Internal error: " + e.getMessage());
        }
    }

    private void handleExistsQuery(Message message, String payload, String subject) {
        try {
            ExistsQueryRequest request = objectMapper.readValue(payload, ExistsQueryRequest.class);
            String resourceId = request.getResourceId();
            String entityType = extractEntityType(subject);

            boolean exists = checkEntityExists(entityType, resourceId);

            ExistsQueryResponse response = new ExistsQueryResponse(request.getQueryId(), exists);
            String responseJson = objectMapper.writeValueAsString(response);

            if (message.getReplyTo() != null) {
                connection.publish(message.getReplyTo(), responseJson.getBytes(StandardCharsets.UTF_8));
                log.debug("Responded to exists query for {} {}: exists={}", entityType, resourceId, exists);
            } else {
                log.warn("Cannot respond to query - no reply subject");
            }
        } catch (Exception e) {
            log.error("Failed to process exists query", e);
            sendErrorResponse(message, "Failed to process exists query: " + e.getMessage());
        }
    }

    private void handleGetQuery(Message message, String payload, String subject) {
        try {
            GetQueryRequest request = objectMapper.readValue(payload, GetQueryRequest.class);
            String resourceId = request.getResourceId();
            String entityType = extractEntityType(subject);

            Object data = getEntityData(entityType, resourceId);

            GetQueryResponse response = new GetQueryResponse(request.getQueryId(), data);
            String responseJson = objectMapper.writeValueAsString(response);

            if (message.getReplyTo() != null) {
                connection.publish(message.getReplyTo(), responseJson.getBytes(StandardCharsets.UTF_8));
                log.debug("Responded to get query for {} {}: found={}", entityType, resourceId, data != null);
            } else {
                log.warn("Cannot respond to get query - no reply subject");
            }
        } catch (Exception e) {
            log.error("Failed to process get query", e);
            sendErrorResponse(message, "Failed to process get query: " + e.getMessage());
        }
    }

    private String extractEntityType(String subject) {
        // Extract entity type from subject like "query.incident.exists" -> "incident"
        // or "query.involved-party.get" -> "involved-party"
        String[] parts = subject.split("\\.");
        if (parts.length >= 2) {
            // Handle both "query.incident.exists" and "prefix.query.incident.exists"
            int entityIndex = parts.length - 2; // Second to last part
            return parts[entityIndex];
        }
        return "unknown";
    }

    private boolean checkEntityExists(String entityType, String resourceId) {
        return switch (entityType) {
            case "incident" -> projectionService.getIncident(resourceId).isPresent();
            case "call" -> projectionService.getCall(resourceId).isPresent();
            case "dispatch" -> projectionService.getDispatch(resourceId).isPresent();
            case "activity" -> projectionService.getActivity(resourceId).isPresent();
            case "assignment" -> projectionService.getAssignment(resourceId).isPresent();
            case "involved-party" -> projectionService.getInvolvedParty(resourceId).isPresent();
            case "resource-assignment" -> {
                try {
                    Long id = Long.parseLong(resourceId);
                    yield projectionService.getResourceAssignment(id).isPresent();
                } catch (NumberFormatException e) {
                    log.warn("Invalid resource assignment ID format: {}", resourceId);
                    yield false;
                }
            }
            default -> {
                log.warn("Unknown entity type: {}", entityType);
                yield false;
            }
        };
    }

    private Object getEntityData(String entityType, String resourceId) {
        return switch (entityType) {
            case "incident" -> projectionService.getIncident(resourceId).orElse(null);
            case "call" -> projectionService.getCall(resourceId).orElse(null);
            case "dispatch" -> projectionService.getDispatch(resourceId).orElse(null);
            case "activity" -> projectionService.getActivity(resourceId).orElse(null);
            case "assignment" -> projectionService.getAssignment(resourceId).orElse(null);
            case "involved-party" -> projectionService.getInvolvedParty(resourceId).orElse(null);
            case "resource-assignment" -> {
                try {
                    Long id = Long.parseLong(resourceId);
                    yield projectionService.getResourceAssignment(id).orElse(null);
                } catch (NumberFormatException e) {
                    log.warn("Invalid resource assignment ID format: {}", resourceId);
                    yield null;
                }
            }
            default -> {
                log.warn("Unknown entity type: {}", entityType);
                yield null;
            }
        };
    }

    private void sendErrorResponse(Message message, String errorMessage) {
        try {
            String queryId = "unknown";
            try {
                String payload = new String(message.getData(), StandardCharsets.UTF_8);
                // Try to parse as either ExistsQueryRequest or GetQueryRequest
                try {
                    ExistsQueryRequest existsRequest = objectMapper.readValue(payload, ExistsQueryRequest.class);
                    queryId = existsRequest.getQueryId();
                } catch (Exception e) {
                    GetQueryRequest getRequest = objectMapper.readValue(payload, GetQueryRequest.class);
                    queryId = getRequest.getQueryId();
                }
            } catch (Exception e) {
                // Ignore - use default queryId
            }

            // Use GetQueryResponse for errors as it's more generic
            GetQueryResponse errorResponse = new GetQueryResponse(queryId, errorMessage);
            String responseJson = objectMapper.writeValueAsString(errorResponse);
            if (message.getReplyTo() != null) {
                connection.publish(message.getReplyTo(), responseJson.getBytes(StandardCharsets.UTF_8));
            } else {
                log.warn("Cannot send error response - no reply subject");
            }
        } catch (Exception e) {
            log.error("Failed to send error response", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (dispatcher != null && querySubjects != null) {
                querySubjects.forEach(subject -> {
                    try {
                        dispatcher.unsubscribe(subject);
                    } catch (Exception e) {
                        log.warn("Error unsubscribing from subject {}", subject, e);
                    }
                });
            }
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            log.warn("Error closing NATS query handler resources", e);
        }
    }
}
