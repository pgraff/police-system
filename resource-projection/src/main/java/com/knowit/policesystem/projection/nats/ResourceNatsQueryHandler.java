package com.knowit.policesystem.projection.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.nats.query.ExistsQueryRequest;
import com.knowit.policesystem.common.nats.query.ExistsQueryResponse;
import com.knowit.policesystem.common.nats.query.GetQueryRequest;
import com.knowit.policesystem.common.nats.query.GetQueryResponse;
import com.knowit.policesystem.projection.api.LocationProjectionResponse;
import com.knowit.policesystem.projection.api.OfficerProjectionResponse;
import com.knowit.policesystem.projection.api.PersonProjectionResponse;
import com.knowit.policesystem.projection.api.UnitProjectionResponse;
import com.knowit.policesystem.projection.api.VehicleProjectionResponse;
import com.knowit.policesystem.projection.config.NatsProperties;
import com.knowit.policesystem.projection.service.ResourceProjectionService;
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

/**
 * NATS query handler for resource projection.
 * Handles synchronous query requests from the edge service for all resource entities.
 */
@Component
public class ResourceNatsQueryHandler {

    private static final Logger log = LoggerFactory.getLogger(ResourceNatsQueryHandler.class);

    private final NatsProperties properties;
    private final ResourceProjectionService projectionService;
    private final ObjectMapper objectMapper;

    private Connection connection;
    private Dispatcher dispatcher;
    private List<String> querySubjects;

    public ResourceNatsQueryHandler(NatsProperties properties,
                                    ResourceProjectionService projectionService,
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

            String prefix = properties.getQuerySubjectPrefix() != null && !properties.getQuerySubjectPrefix().isBlank()
                    ? properties.getQuerySubjectPrefix()
                    : "query";

            querySubjects = new ArrayList<>();
            querySubjects.add(prefix + ".officer.>");
            querySubjects.add(prefix + ".vehicle.>");
            querySubjects.add(prefix + ".unit.>");
            querySubjects.add(prefix + ".person.>");
            querySubjects.add(prefix + ".location.>");

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
        // Extract entity type from subject like "query.officer.exists" -> "officer"
        String[] parts = subject.split("\\.");
        if (parts.length >= 2) {
            // Handle both "query.officer.exists" and "prefix.query.officer.exists"
            int entityIndex = parts.length - 2; // Second to last part
            return parts[entityIndex];
        }
        return "unknown";
    }

    private boolean checkEntityExists(String entityType, String resourceId) {
        return switch (entityType) {
            case "officer" -> projectionService.getOfficer(resourceId).isPresent();
            case "vehicle" -> projectionService.getVehicle(resourceId).isPresent();
            case "unit" -> projectionService.getUnit(resourceId).isPresent();
            case "person" -> projectionService.getPerson(resourceId).isPresent();
            case "location" -> projectionService.getLocation(resourceId).isPresent();
            default -> {
                log.warn("Unknown entity type: {}", entityType);
                yield false;
            }
        };
    }

    private Object getEntityData(String entityType, String resourceId) {
        return switch (entityType) {
            case "officer" -> projectionService.getOfficer(resourceId).orElse(null);
            case "vehicle" -> projectionService.getVehicle(resourceId).orElse(null);
            case "unit" -> projectionService.getUnit(resourceId).orElse(null);
            case "person" -> projectionService.getPerson(resourceId).orElse(null);
            case "location" -> projectionService.getLocation(resourceId).orElse(null);
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
            log.error("Error shutting down NATS query handler", e);
        }
    }
}
