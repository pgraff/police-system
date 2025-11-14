package com.knowit.policesystem.common.events;

import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * Interface for publishing events to the event bus (Kafka).
 * Provides methods to publish events to topics with optional partitioning keys.
 */
public interface EventPublisher {

    /**
     * Callback interface for handling publish results.
     * Allows the application to react to both successful and failed event publications.
     */
    interface PublishCallback {
        /**
         * Called when an event is successfully published.
         *
         * @param event the event that was published
         * @param metadata the Kafka record metadata (topic, partition, offset)
         */
        void onSuccess(Event event, RecordMetadata metadata);

        /**
         * Called when an event publication fails.
         *
         * @param event the event that failed to publish
         * @param exception the exception that caused the failure
         */
        void onFailure(Event event, Exception exception);
    }

    /**
     * Publishes an event to the specified topic.
     * Uses the event's aggregateId as the partition key.
     * Note: This method does not provide feedback on success/failure.
     * Use {@link #publish(String, Event, PublishCallback)} if you need to handle the result.
     *
     * @param topic the Kafka topic to publish to
     * @param event the event to publish
     */
    void publish(String topic, Event event);

    /**
     * Publishes an event to the specified topic with a custom partition key.
     * Note: This method does not provide feedback on success/failure.
     * Use {@link #publish(String, String, Event, PublishCallback)} if you need to handle the result.
     *
     * @param topic the Kafka topic to publish to
     * @param key the partition key (typically the aggregateId)
     * @param event the event to publish
     */
    void publish(String topic, String key, Event event);

    /**
     * Publishes an event to the specified topic with a callback to handle the result.
     * Uses the event's aggregateId as the partition key.
     *
     * @param topic the Kafka topic to publish to
     * @param event the event to publish
     * @param callback callback to handle success or failure
     */
    void publish(String topic, Event event, PublishCallback callback);

    /**
     * Publishes an event to the specified topic with a custom partition key and callback.
     *
     * @param topic the Kafka topic to publish to
     * @param key the partition key (typically the aggregateId)
     * @param event the event to publish
     * @param callback callback to handle success or failure
     */
    void publish(String topic, String key, Event event, PublishCallback callback);
}

