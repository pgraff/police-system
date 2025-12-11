package com.knowit.policesystem.edge.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for testing NATS JetStream in integration tests.
 * Provides utilities to consume messages from NATS subjects and verify events.
 */
public class NatsTestHelper implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(NatsTestHelper.class);

    private final Connection connection;
    private final JetStream jetStream;
    private final JetStreamManagement jetStreamManagement;
    private final ObjectMapper objectMapper;
    private final List<JetStreamSubscription> subscriptions = new ArrayList<>();

    /**
     * Creates a new NatsTestHelper connected to the specified NATS URL.
     *
     * @param natsUrl NATS server URL (e.g., "nats://localhost:4222")
     * @param objectMapper ObjectMapper for deserializing events
     * @throws IOException if connection fails
     * @throws InterruptedException if connection is interrupted
     */
    public NatsTestHelper(String natsUrl, ObjectMapper objectMapper) throws IOException, InterruptedException {
        this.objectMapper = objectMapper;
        Options options = new Options.Builder()
                .server(natsUrl)
                .connectionTimeout(Duration.ofSeconds(5))
                .build();
        this.connection = Nats.connect(options);
        this.jetStream = connection.jetStream();
        this.jetStreamManagement = connection.jetStreamManagement();
        logger.info("Connected to NATS at {}", natsUrl);
    }

    /**
     * Consumes a message from the specified subject, waiting up to the specified timeout.
     * Creates a stream if needed and uses a push consumer.
     *
     * @param subject NATS subject to consume from
     * @param timeout maximum time to wait for a message
     * @return the message content as a string, or null if no message received
     * @throws IOException if an error occurs
     * @throws InterruptedException if interrupted
     */
    /**
     * Ensures a stream exists for the given subject pattern.
     * This should be called before publishing to ensure the stream exists.
     */
    public void ensureStreamForSubject(String subject) throws IOException, InterruptedException {
        try {
            // Check if catch-all stream exists first
            String catchAllStreamName = "test-stream-commands-all";
            try {
                StreamInfo catchAllInfo = jetStreamManagement.getStreamInfo(catchAllStreamName);
                // If catch-all stream exists and matches our subject, we're done
                if (catchAllInfo.getConfiguration().getSubjects().contains("commands.>")) {
                    logger.debug("Catch-all stream {} already exists and covers subject {}", catchAllStreamName, subject);
                    return;
                }
            } catch (Exception e) {
                // Catch-all doesn't exist, will create specific or catch-all
            }
            
            // Create a stream name based on the subject
            // Replace special characters to create a valid stream name
            String streamName = "test-stream-" + subject.replace(".", "-").replace("*", "all").replace(">", "all");
            try {
                jetStreamManagement.getStreamInfo(streamName);
                logger.debug("Stream {} already exists", streamName);
            } catch (Exception e) {
                // Stream doesn't exist, create it
                // Use the subject pattern as-is (supports wildcards like "commands.>")
                StreamConfiguration streamConfig = StreamConfiguration.builder()
                        .name(streamName)
                        .subjects(subject)
                        .maxAge(Duration.ofHours(1))
                        .build();
                jetStreamManagement.addStream(streamConfig);
                logger.info("Created stream {} for subject pattern {}", streamName, subject);
            }
        } catch (Exception e) {
            // If subjects overlap, that's okay - another stream already covers this subject
            if (e.getMessage() != null && (e.getMessage().contains("subjects overlap") || 
                    e.getMessage().contains("stream name already in use"))) {
                logger.debug("Stream with overlapping subjects already exists, continuing");
            } else {
                logger.warn("Error ensuring stream for subject {}", subject, e);
                throw new IOException("Failed to ensure stream for NATS subject: " + subject, e);
            }
        }
    }

    public String consumeMessage(String subject, Duration timeout) throws IOException, InterruptedException {
        try {
            // Ensure stream exists - try to find existing stream first
            String streamName = findOrCreateStreamForSubject(subject);
            
            // Create a pull subscription with durable consumer
            // Use a consistent consumer name per test run to allow message replay
            String consumerName = "test-consumer-" + streamName;
            PullSubscribeOptions options = PullSubscribeOptions.builder()
                    .stream(streamName)
                    .durable(consumerName)
                    .build();
            
            JetStreamSubscription sub = jetStream.subscribe(subject, options);
            subscriptions.add(sub);
            
            // Small delay to allow any pending async publishes to complete
            Thread.sleep(500);
            
            // Request messages from the pull subscription
            sub.pull(1);
            
            // Get message with timeout
            Message msg = sub.nextMessage(timeout);
            if (msg != null) {
                String content = new String(msg.getData());
                msg.ack();
                return content;
            }
        } catch (Exception e) {
            logger.warn("Error consuming message from subject {}", subject, e);
            throw new IOException("Failed to consume message from NATS subject: " + subject, e);
        }
        return null;
    }
    
    /**
     * Finds an existing stream that matches the subject, or creates a new one.
     */
    private String findOrCreateStreamForSubject(String subject) throws IOException, InterruptedException {
        // First, try to find existing stream with wildcard pattern
        String catchAllStreamName = "test-stream-commands-all";
        try {
            StreamInfo info = jetStreamManagement.getStreamInfo(catchAllStreamName);
            // Check if this stream matches our subject (catch-all "commands.>" matches all command subjects)
            if (info.getConfiguration().getSubjects().contains("commands.>")) {
                return catchAllStreamName;
            }
        } catch (Exception e) {
            // Stream doesn't exist, will check for specific
        }
        
        // Try to find stream for specific subject
        String specificStreamName = "test-stream-" + subject.replace(".", "-");
        try {
            jetStreamManagement.getStreamInfo(specificStreamName);
            return specificStreamName;
        } catch (Exception e) {
            // No specific stream exists, use catch-all if it exists, otherwise create specific
            // But since we already checked catch-all, just return it (it should exist from setUp)
            return catchAllStreamName;
        }
    }

    /**
     * Prepares a subscription for a subject (creates stream and subscribes).
     * Call this before publishing to ensure you don't miss messages.
     *
     * @param subject NATS subject to prepare subscription for
     * @return the subscription (can be used to fetch messages later)
     * @throws IOException if an error occurs
     * @throws InterruptedException if interrupted
     */
    public JetStreamSubscription prepareSubscription(String subject) throws IOException, InterruptedException {
        // Ensure stream exists - this must happen before publishing
        ensureStreamForSubject(subject);
        
        // Find or get stream name
        String streamName = findOrCreateStreamForSubject(subject);
        
        // Create a pull subscription with a unique consumer name to avoid conflicts
        String consumerName = "test-consumer-" + streamName + "-" + System.currentTimeMillis();
        PullSubscribeOptions options = PullSubscribeOptions.builder()
                .stream(streamName)
                .durable(consumerName)
                .build();
        
        try {
            JetStreamSubscription sub = jetStream.subscribe(subject, options);
            subscriptions.add(sub);
            logger.debug("Prepared subscription for subject {} on stream {}", subject, streamName);
            return sub;
        } catch (Exception e) {
            logger.warn("Failed to create subscription for subject {}, will retry without durable consumer", subject, e);
            // Retry without durable consumer
            try {
                PullSubscribeOptions retryOptions = PullSubscribeOptions.builder()
                        .stream(streamName)
                        .build();
                JetStreamSubscription sub = jetStream.subscribe(subject, retryOptions);
                subscriptions.add(sub);
                return sub;
            } catch (Exception e2) {
                logger.error("Failed to create subscription even without durable consumer", e2);
                throw new IOException("Failed to create NATS subscription for subject: " + subject, e2);
            }
        }
    }
    
    /**
     * Verifies that a message exists in the stream for the given subject.
     * This is an alternative to consuming the message - it checks stream state.
     */
    public boolean verifyMessageInStream(String subject) throws IOException, InterruptedException {
        try {
            String streamName = findOrCreateStreamForSubject(subject);
            StreamInfo streamInfo = jetStreamManagement.getStreamInfo(streamName);
            // Check if stream has any messages
            return streamInfo.getStreamState().getMsgCount() > 0;
        } catch (Exception e) {
            logger.warn("Error verifying message in stream for subject {}", subject, e);
            return false;
        }
    }

    /**
     * Consumes and deserializes an event from the specified subject.
     *
     * @param subject NATS subject to consume from
     * @param eventClass the event class to deserialize to
     * @param timeout maximum time to wait for a message
     * @param <T> the event type
     * @return the deserialized event, or null if no message received
     * @throws IOException if an error occurs
     * @throws InterruptedException if interrupted
     */
    public <T> T consumeEvent(String subject, Class<T> eventClass, Duration timeout) 
            throws IOException, InterruptedException {
        String messageContent = consumeMessage(subject, timeout);
        if (messageContent != null) {
            return objectMapper.readValue(messageContent, eventClass);
        }
        return null;
    }

    @Override
    public void close() {
        // Unsubscribe from all subscriptions
        for (JetStreamSubscription sub : subscriptions) {
            try {
                sub.unsubscribe();
            } catch (Exception e) {
                logger.warn("Error unsubscribing from NATS subject", e);
            }
        }
        
        // Close connection
        if (connection != null && connection.getStatus() != Connection.Status.CLOSED) {
            try {
                connection.close();
                logger.info("Closed NATS connection");
            } catch (InterruptedException e) {
                logger.warn("Interrupted while closing NATS connection", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}
