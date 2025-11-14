package com.knowit.policesystem.common.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Kafka implementation of EventPublisher.
 * Publishes events to Kafka topics as JSON messages.
 */
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final Producer<String, String> producer;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new KafkaEventPublisher with the given producer properties and ObjectMapper.
     *
     * @param producerProperties Kafka producer configuration properties
     * @param objectMapper Jackson ObjectMapper for JSON serialization
     */
    public KafkaEventPublisher(Properties producerProperties, ObjectMapper objectMapper) {
        this.producer = new KafkaProducer<>(producerProperties);
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String topic, Event event) {
        // Use aggregateId as the partition key
        publish(topic, event.getAggregateId(), event);
    }

    @Override
    public void publish(String topic, String key, Event event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, json);
            
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Failed to publish event {} to topic {}", event.getEventId(), topic, exception);
                } else {
                    logger.debug("Published event {} to topic {} partition {} offset {}", 
                            event.getEventId(), metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event {} to JSON", event.getEventId(), e);
            throw new EventPublishingException("Failed to serialize event to JSON", e);
        }
    }

    /**
     * Closes the Kafka producer.
     * Should be called when the publisher is no longer needed.
     */
    public void close() {
        if (producer != null) {
            producer.close();
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

