package com.knowit.policesystem.common.events;

/**
 * Interface for publishing events to the event bus (Kafka).
 * Provides methods to publish events to topics with optional partitioning keys.
 */
public interface EventPublisher {

    /**
     * Publishes an event to the specified topic.
     * Uses the event's aggregateId as the partition key.
     *
     * @param topic the Kafka topic to publish to
     * @param event the event to publish
     */
    void publish(String topic, Event event);

    /**
     * Publishes an event to the specified topic with a custom partition key.
     *
     * @param topic the Kafka topic to publish to
     * @param key the partition key (typically the aggregateId)
     * @param event the event to publish
     */
    void publish(String topic, String key, Event event);
}

