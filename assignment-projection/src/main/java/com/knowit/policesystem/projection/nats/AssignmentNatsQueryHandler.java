package com.knowit.policesystem.projection.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.nats.query.ExistsQueryRequest;
import com.knowit.policesystem.common.nats.query.ExistsQueryResponse;
import com.knowit.policesystem.common.nats.query.GetQueryRequest;
import com.knowit.policesystem.common.nats.query.GetQueryResponse;
import com.knowit.policesystem.projection.config.NatsProperties;
import com.knowit.policesystem.projection.service.AssignmentProjectionService;
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

/**
 * NATS query handler for assignment projection.
 * Handles synchronous query requests from the edge service.
 */
@Component
public class AssignmentNatsQueryHandler {

    private static final Logger log = LoggerFactory.getLogger(AssignmentNatsQueryHandler.class);

    private final NatsProperties properties;
    private final AssignmentProjectionService projectionService;
    private final ObjectMapper objectMapper;

    private Connection connection;
    private Dispatcher dispatcher;
    private String querySubject;

    public AssignmentNatsQueryHandler(NatsProperties properties,
                                       AssignmentProjectionService projectionService,
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
                    ? "query.assignment" 
                    : properties.getQuerySubjectPrefix() + ".assignment";
            querySubject = prefix + ".>";
            
            dispatcher.subscribe(querySubject);
            log.info("Subscribed to NATS query subject {}", querySubject);
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
                handleExistsQuery(message, payload);
            } else if (subject.endsWith(".get")) {
                handleGetQuery(message, payload);
            } else {
                log.warn("Unknown query operation for subject: {}", subject);
                sendErrorResponse(message, "Unknown query operation: " + subject);
            }
        } catch (Exception e) {
            log.error("Failed to handle query request", e);
            sendErrorResponse(message, "Internal error: " + e.getMessage());
        }
    }

    private void handleExistsQuery(Message message, String payload) {
        try {
            ExistsQueryRequest request = objectMapper.readValue(payload, ExistsQueryRequest.class);
            String assignmentId = request.getResourceId();
            
            boolean exists = projectionService.getProjection(assignmentId).isPresent();
            
            ExistsQueryResponse response = new ExistsQueryResponse(request.getQueryId(), exists);
            String responseJson = objectMapper.writeValueAsString(response);
            
            if (message.getReplyTo() != null) {
                connection.publish(message.getReplyTo(), responseJson.getBytes(StandardCharsets.UTF_8));
                log.debug("Responded to exists query for assignmentId {}: exists={}", assignmentId, exists);
            } else {
                log.warn("Cannot respond to query - no reply subject");
            }
        } catch (Exception e) {
            log.error("Failed to process exists query", e);
            sendErrorResponse(message, "Failed to process exists query: " + e.getMessage());
        }
    }

    private void handleGetQuery(Message message, String payload) {
        try {
            GetQueryRequest request = objectMapper.readValue(payload, GetQueryRequest.class);
            String assignmentId = request.getResourceId();
            
            Object data = projectionService.getProjection(assignmentId)
                    .orElse(null); // Return null if not found
            
            GetQueryResponse response = new GetQueryResponse(request.getQueryId(), data);
            String responseJson = objectMapper.writeValueAsString(response);
            
            if (message.getReplyTo() != null) {
                connection.publish(message.getReplyTo(), responseJson.getBytes(StandardCharsets.UTF_8));
                log.debug("Responded to get query for assignmentId {}: found={}", assignmentId, data != null);
            } else {
                log.warn("Cannot respond to get query - no reply subject");
            }
        } catch (Exception e) {
            log.error("Failed to process get query", e);
            sendErrorResponse(message, "Failed to process get query: " + e.getMessage());
        }
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
            if (dispatcher != null && querySubject != null) {
                dispatcher.unsubscribe(querySubject);
            }
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            log.warn("Error closing NATS query handler resources", e);
        }
    }
}

