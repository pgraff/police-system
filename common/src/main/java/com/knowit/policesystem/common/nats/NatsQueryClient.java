package com.knowit.policesystem.common.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.nats.query.QueryRequest;
import com.knowit.policesystem.common.nats.query.QueryResponse;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Client for sending synchronous query requests to projections via NATS.
 * Supports request-response pattern for querying projection state.
 */
public class NatsQueryClient {

    private static final Logger logger = LoggerFactory.getLogger(NatsQueryClient.class);

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final long timeoutMillis;
    private final boolean enabled;

    /**
     * Creates a new NatsQueryClient with the given connection options and ObjectMapper.
     * Supports both single server URL and comma-separated multiple server URLs for high availability.
     *
     * @param natsUrl NATS server URL(s) (e.g., "nats://localhost:4222" or "nats://localhost:4222,nats://localhost:4223,nats://localhost:4224")
     * @param objectMapper Jackson ObjectMapper for JSON serialization
     * @param timeoutMillis timeout in milliseconds for query requests
     * @param enabled whether NATS query client is enabled
     */
    public NatsQueryClient(String natsUrl, ObjectMapper objectMapper, long timeoutMillis, boolean enabled) {
        this.objectMapper = objectMapper;
        this.timeoutMillis = timeoutMillis;
        this.enabled = enabled;

        if (!enabled) {
            this.natsConnection = null;
            logger.info("NATS query client is disabled");
            return;
        }

        try {
            // Parse comma-separated URLs if multiple servers are specified
            String[] serverUrls = natsUrl.split(",");
            Options.Builder optionsBuilder = new Options.Builder()
                    .connectionTimeout(Duration.ofSeconds(5))
                    .reconnectWait(Duration.ofSeconds(1))
                    .maxReconnects(-1); // Unlimited reconnects

            if (serverUrls.length == 1) {
                // Single server
                optionsBuilder.server(serverUrls[0].trim());
                logger.info("Connecting to NATS server at {} for queries", serverUrls[0].trim());
            } else {
                // Multiple servers for high availability
                String[] trimmedUrls = new String[serverUrls.length];
                for (int i = 0; i < serverUrls.length; i++) {
                    trimmedUrls[i] = serverUrls[i].trim();
                }
                optionsBuilder.servers(trimmedUrls);
                logger.info("Connecting to NATS cluster with {} servers for queries: {}", trimmedUrls.length, String.join(", ", trimmedUrls));
            }

            Options options = optionsBuilder.build();
            this.natsConnection = Nats.connect(options);
            logger.info("Successfully connected to NATS cluster for queries");
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to connect to NATS server(s) at {} for queries", natsUrl, e);
            throw new RuntimeException("Failed to initialize NATS query client connection", e);
        }
    }

    /**
     * Sends a query request to a projection and waits for a response.
     *
     * @param subject the NATS subject to send the request to (e.g., "query.officer.exists")
     * @param request the query request
     * @param responseClass the expected response class
     * @param <T> the response type
     * @return the query response
     * @throws NatsQueryException if the query fails or times out
     */
    public <T extends QueryResponse> T query(String subject, QueryRequest request, Class<T> responseClass) throws NatsQueryException {
        if (!enabled || natsConnection == null) {
            throw new NatsQueryException("NATS query client is disabled or not connected");
        }

        try {
            // Ensure query has an ID
            if (request.getQueryId() == null || request.getQueryId().isEmpty()) {
                request.setQueryId(UUID.randomUUID().toString());
            }

            // Serialize request to JSON
            String requestJson = objectMapper.writeValueAsString(request);
            byte[] requestBytes = requestJson.getBytes(StandardCharsets.UTF_8);

            logger.debug("Sending query request to subject {}: {}", subject, requestJson);

            // Send request and wait for response
            Message response;
            try {
                response = natsConnection.request(subject, requestBytes, Duration.ofMillis(timeoutMillis));
            } catch (IllegalStateException e) {
                // Connection closed or other connection issues
                throw new NatsQueryException("NATS connection error: " + e.getMessage(), e);
            }

            if (response == null) {
                throw new NatsQueryException("Query request to " + subject + " timed out after " + timeoutMillis + "ms");
            }

            // Deserialize response
            String responseJson = new String(response.getData(), StandardCharsets.UTF_8);
            logger.debug("Received query response from subject {}: {}", subject, responseJson);

            T queryResponse = objectMapper.readValue(responseJson, responseClass);

            if (!queryResponse.isSuccess()) {
                throw new NatsQueryException("Query failed: " + queryResponse.getErrorMessage());
            }

            return queryResponse;
        } catch (NatsQueryException e) {
            // Re-throw NatsQueryException as-is
            throw e;
        } catch (IOException e) {
            throw new NatsQueryException("Failed to serialize/deserialize query request/response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NatsQueryException("Query request was interrupted", e);
        } catch (Exception e) {
            // Catch any other exceptions (e.g., timeout exceptions from NATS client)
            if (e.getMessage() != null && (e.getMessage().contains("timeout") || e.getMessage().contains("timed out"))) {
                throw new NatsQueryException("Query request to " + subject + " timed out after " + timeoutMillis + "ms", e);
            }
            throw new NatsQueryException("Query request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a query request asynchronously.
     *
     * @param subject the NATS subject to send the request to
     * @param request the query request
     * @param responseClass the expected response class
     * @param <T> the response type
     * @return CompletableFuture with the query response
     */
    public <T extends QueryResponse> CompletableFuture<T> queryAsync(String subject, QueryRequest request, Class<T> responseClass) {
        if (!enabled || natsConnection == null) {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(new NatsQueryException("NATS query client is disabled or not connected"));
            return future;
        }

        CompletableFuture<T> future = new CompletableFuture<>();

        try {
            // Ensure query has an ID
            if (request.getQueryId() == null || request.getQueryId().isEmpty()) {
                request.setQueryId(UUID.randomUUID().toString());
            }

            // Serialize request to JSON
            String requestJson = objectMapper.writeValueAsString(request);
            byte[] requestBytes = requestJson.getBytes(StandardCharsets.UTF_8);

            logger.debug("Sending async query request to subject {}: {}", subject, requestJson);

            // Send request asynchronously
            natsConnection.request(subject, requestBytes)
                    .thenApply(response -> {
                        try {
                            String responseJson = new String(response.getData(), StandardCharsets.UTF_8);
                            logger.debug("Received async query response from subject {}: {}", subject, responseJson);
                            T queryResponse = objectMapper.readValue(responseJson, responseClass);
                            if (!queryResponse.isSuccess()) {
                                future.completeExceptionally(new NatsQueryException("Query failed: " + queryResponse.getErrorMessage()));
                                return null;
                            }
                            future.complete(queryResponse);
                            return queryResponse;
                        } catch (IOException e) {
                            future.completeExceptionally(new NatsQueryException("Failed to deserialize query response", e));
                            return null;
                        }
                    })
                    .exceptionally(throwable -> {
                        future.completeExceptionally(new NatsQueryException("Query request failed", throwable));
                        return null;
                    });
        } catch (IOException e) {
            future.completeExceptionally(new NatsQueryException("Failed to serialize query request", e));
        }

        return future;
    }

    /**
     * Closes the NATS connection.
     * Should be called when the client is no longer needed.
     */
    public void close() {
        if (natsConnection != null && natsConnection.getStatus() != Connection.Status.CLOSED) {
            try {
                natsConnection.close();
                logger.info("Closed NATS query client connection");
            } catch (InterruptedException e) {
                logger.warn("Interrupted while closing NATS query client connection", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Exception thrown when a query request fails.
     */
    public static class NatsQueryException extends Exception {
        public NatsQueryException(String message) {
            super(message);
        }

        public NatsQueryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

