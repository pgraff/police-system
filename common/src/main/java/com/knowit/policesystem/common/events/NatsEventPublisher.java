package com.knowit.policesystem.common.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.PublishAck;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * NATS JetStream implementation of EventPublisher.
 * Publishes events to NATS JetStream subjects as JSON messages.
 */
public class NatsEventPublisher implements EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NatsEventPublisher.class);

    private final Connection natsConnection;
    private final JetStream jetStream;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    /**
     * Creates a new NatsEventPublisher with the given connection options and ObjectMapper.
     *
     * @param natsUrl NATS server URL (e.g., "nats://localhost:4222")
     * @param objectMapper Jackson ObjectMapper for JSON serialization
     * @param enabled whether NATS publishing is enabled
     */
    public NatsEventPublisher(String natsUrl, ObjectMapper objectMapper, boolean enabled) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        
        if (!enabled) {
            this.natsConnection = null;
            this.jetStream = null;
            logger.info("NATS publishing is disabled");
            return;
        }
        
        try {
            Options options = new Options.Builder()
                    .server(natsUrl)
                    .connectionTimeout(Duration.ofSeconds(5))
                    .reconnectWait(Duration.ofSeconds(1))
                    .maxReconnects(-1) // Unlimited reconnects
                    .build();
            
            this.natsConnection = Nats.connect(options);
            this.jetStream = natsConnection.jetStream();
            logger.info("Connected to NATS server at {}", natsUrl);
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to connect to NATS server at {}", natsUrl, e);
            throw new RuntimeException("Failed to initialize NATS connection", e);
        }
    }

    @Override
    public void publish(String subject, Event event) {
        publish(subject, event.getAggregateId(), event, createDefaultCallback(subject));
    }

    @Override
    public void publish(String subject, String key, Event event) {
        publish(subject, key, event, createDefaultCallback(subject));
    }

    @Override
    public void publish(String subject, Event event, PublishCallback callback) {
        publish(subject, event.getAggregateId(), event, callback);
    }

    @Override
    public void publish(String subject, String key, Event event, PublishCallback callback) {
        if (!enabled) {
            logger.debug("NATS publishing is disabled, skipping event {}", event.getEventId());
            // Create a mock RecordMetadata for compatibility
            RecordMetadata mockMetadata = createMockRecordMetadata(subject, key);
            callback.onSuccess(event, mockMetadata);
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(event);
            byte[] messageBytes = json.getBytes(StandardCharsets.UTF_8);
            
            // Publish to JetStream with subject
            CompletableFuture<PublishAck> publishFuture = jetStream.publishAsync(subject, messageBytes);
            
            publishFuture.whenComplete((ack, exception) -> {
                if (exception != null) {
                    logger.error("Failed to publish event {} to NATS subject {}", event.getEventId(), subject, exception);
                    callback.onFailure(event, new EventPublishingException("Failed to publish event to NATS", exception));
                } else {
                    logger.debug("Published event {} to NATS subject {} with sequence {}", 
                            event.getEventId(), subject, ack.getSeqno());
                    // Create a mock RecordMetadata for compatibility with EventPublisher interface
                    RecordMetadata mockMetadata = createMockRecordMetadata(subject, key);
                    callback.onSuccess(event, mockMetadata);
                }
            });
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event {} to JSON", event.getEventId(), e);
            callback.onFailure(event, new EventPublishingException("Failed to serialize event to JSON", e));
        }
    }

    /**
     * Creates a minimal RecordMetadata-like object for compatibility with EventPublisher interface.
     * NATS doesn't have the same concept as Kafka's RecordMetadata, so we return null.
     * Callbacks should handle null metadata gracefully when working with NATS.
     */
    private RecordMetadata createMockRecordMetadata(String subject, String key) {
        // NATS doesn't have RecordMetadata, so we return null
        // The callback implementations should handle null metadata gracefully
        return null;
    }

    /**
     * Creates a default callback that logs success and failure.
     * Used by the fire-and-forget publish methods.
     */
    private PublishCallback createDefaultCallback(String subject) {
        return new PublishCallback() {
            @Override
            public void onSuccess(Event event, RecordMetadata metadata) {
                logger.debug("Published event {} to NATS subject {}", event.getEventId(), subject);
            }

            @Override
            public void onFailure(Event event, Exception exception) {
                logger.error("Failed to publish event {} to NATS subject {}", event.getEventId(), subject, exception);
            }
        };
    }

    /**
     * Closes the NATS connection.
     * Should be called when the publisher is no longer needed.
     */
    public void close() {
        if (natsConnection != null && natsConnection.getStatus() != Connection.Status.CLOSED) {
            try {
                natsConnection.close();
                logger.info("Closed NATS connection");
            } catch (InterruptedException e) {
                logger.warn("Interrupted while closing NATS connection", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Exception thrown when event publishing fails.
     */
    public static class EventPublishingException extends RuntimeException {
        public EventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
