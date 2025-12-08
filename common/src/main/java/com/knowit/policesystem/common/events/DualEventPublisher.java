package com.knowit.policesystem.common.events;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Composite EventPublisher that implements the double-publish pattern.
 * 
 * All events are published to Kafka (for event sourcing and long-term storage).
 * Critical events are also published to NATS/JetStream (for near realtime processing).
 * 
 * Event classification determines which events are critical:
 * - All command events (ending with "Requested") are considered critical
 * - Non-critical events are published only to Kafka
 */
public class DualEventPublisher implements EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DualEventPublisher.class);

    private final EventPublisher kafkaPublisher;
    private final EventPublisher natsPublisher;

    /**
     * Creates a new DualEventPublisher with Kafka and NATS publishers.
     *
     * @param kafkaPublisher the Kafka event publisher (must not be null)
     * @param natsPublisher the NATS event publisher (may be null if NATS is disabled)
     */
    public DualEventPublisher(EventPublisher kafkaPublisher, EventPublisher natsPublisher) {
        if (kafkaPublisher == null) {
            throw new IllegalArgumentException("Kafka publisher cannot be null");
        }
        this.kafkaPublisher = kafkaPublisher;
        this.natsPublisher = natsPublisher;
    }

    @Override
    public void publish(String topic, Event event) {
        publish(topic, event.getAggregateId(), event);
    }

    @Override
    public void publish(String topic, String key, Event event) {
        // Always publish to Kafka
        kafkaPublisher.publish(topic, key, event);
        
        // Publish to NATS/JetStream if event is critical
        if (EventClassification.isCritical(event) && natsPublisher != null) {
            String natsSubject = EventClassification.generateNatsSubject(event);
            logger.debug("Publishing critical event {} to NATS subject {}", event.getEventId(), natsSubject);
            natsPublisher.publish(natsSubject, key, event);
        }
    }

    @Override
    public void publish(String topic, Event event, PublishCallback callback) {
        publish(topic, event.getAggregateId(), event, callback);
    }

    @Override
    public void publish(String topic, String key, Event event, PublishCallback callback) {
        // Create a composite callback that handles both publishers
        CompositeCallback compositeCallback = new CompositeCallback(callback, event, topic);
        
        // Always publish to Kafka
        kafkaPublisher.publish(topic, key, event, compositeCallback.getKafkaCallback());
        
        // Publish to NATS/JetStream if event is critical
        if (EventClassification.isCritical(event) && natsPublisher != null) {
            String natsSubject = EventClassification.generateNatsSubject(event);
            logger.debug("Publishing critical event {} to NATS subject {}", event.getEventId(), natsSubject);
            natsPublisher.publish(natsSubject, key, event, compositeCallback.getNatsCallback());
        } else {
            // If not critical or NATS is disabled, mark NATS as complete immediately
            compositeCallback.markNatsComplete();
        }
    }

    /**
     * Composite callback that waits for both Kafka and NATS to complete before calling the original callback.
     */
    private static class CompositeCallback {
        private final PublishCallback originalCallback;
        private final Event event;
        private final String topic;
        private volatile boolean kafkaComplete = false;
        private volatile boolean natsComplete = false;
        private volatile RecordMetadata kafkaMetadata = null;
        private volatile Exception kafkaException = null;
        private volatile Exception natsException = null;

        CompositeCallback(PublishCallback originalCallback, Event event, String topic) {
            this.originalCallback = originalCallback;
            this.event = event;
            this.topic = topic;
        }

        PublishCallback getKafkaCallback() {
            return new PublishCallback() {
                @Override
                public void onSuccess(Event event, RecordMetadata metadata) {
                    kafkaMetadata = metadata;
                    kafkaComplete = true;
                    checkAndCallOriginal();
                }

                @Override
                public void onFailure(Event event, Exception exception) {
                    kafkaException = exception;
                    kafkaComplete = true;
                    checkAndCallOriginal();
                }
            };
        }

        PublishCallback getNatsCallback() {
            return new PublishCallback() {
                @Override
                public void onSuccess(Event event, RecordMetadata metadata) {
                    natsComplete = true;
                    checkAndCallOriginal();
                }

                @Override
                public void onFailure(Event event, Exception exception) {
                    natsException = exception;
                    natsComplete = true;
                    checkAndCallOriginal();
                }
            };
        }

        void markNatsComplete() {
            natsComplete = true;
            checkAndCallOriginal();
        }

        private synchronized void checkAndCallOriginal() {
            if (kafkaComplete && natsComplete) {
                // If Kafka failed, that's the primary failure
                if (kafkaException != null) {
                    originalCallback.onFailure(event, kafkaException);
                } else if (natsException != null) {
                    // NATS failure is secondary, but we still report it
                    // Kafka succeeded, so we consider it a partial success
                    logger.warn("Kafka publish succeeded but NATS publish failed for event {}", event.getEventId(), natsException);
                    originalCallback.onSuccess(event, kafkaMetadata);
                } else {
                    // Both succeeded
                    originalCallback.onSuccess(event, kafkaMetadata);
                }
            }
        }
    }
}
