package com.knowit.policesystem.edge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.DualEventPublisher;
import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.KafkaEventPublisher;
import com.knowit.policesystem.common.events.NatsEventPublisher;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Event publishing configuration for Kafka and NATS/JetStream.
 * Configures DualEventPublisher bean that implements double-publish pattern:
 * - All events are published to Kafka (for event sourcing)
 * - Critical events are also published to NATS/JetStream (for near realtime processing)
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${nats.url:nats://localhost:4222}")
    private String natsUrl;

    @Value("${nats.enabled:true}")
    private boolean natsEnabled;

    /**
     * Creates an ObjectMapper configured for event serialization.
     * Includes JavaTimeModule for Instant serialization.
     *
     * @return configured ObjectMapper
     */
    @Bean
    public ObjectMapper eventObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Creates a KafkaEventPublisher bean for publishing events to Kafka.
     *
     * @param eventObjectMapper the ObjectMapper for event serialization
     * @return KafkaEventPublisher implementation
     */
    @Bean
    public KafkaEventPublisher kafkaEventPublisher(ObjectMapper eventObjectMapper) {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        producerProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

        return new KafkaEventPublisher(producerProps, eventObjectMapper);
    }

    /**
     * Creates a NatsEventPublisher bean for publishing critical events to NATS/JetStream.
     *
     * @param eventObjectMapper the ObjectMapper for event serialization
     * @return NatsEventPublisher implementation (or null if NATS is disabled)
     */
    @Bean
    public NatsEventPublisher natsEventPublisher(ObjectMapper eventObjectMapper) {
        if (!natsEnabled) {
            return new NatsEventPublisher(natsUrl, eventObjectMapper, false);
        }
        return new NatsEventPublisher(natsUrl, eventObjectMapper, true);
    }

    /**
     * Creates a DualEventPublisher bean that implements the double-publish pattern.
     * All events are published to Kafka, and critical events are also published to NATS/JetStream.
     *
     * @param kafkaEventPublisher the Kafka event publisher
     * @param natsEventPublisher the NATS event publisher (may be disabled)
     * @return DualEventPublisher implementation
     */
    @Bean
    public EventPublisher eventPublisher(KafkaEventPublisher kafkaEventPublisher, NatsEventPublisher natsEventPublisher) {
        return new DualEventPublisher(kafkaEventPublisher, natsEventPublisher);
    }
}
