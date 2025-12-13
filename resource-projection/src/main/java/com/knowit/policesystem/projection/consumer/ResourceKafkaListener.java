package com.knowit.policesystem.projection.consumer;

import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.projection.service.ResourceProjectionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ResourceKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(ResourceKafkaListener.class);

    private final ResourceEventParser parser;
    private final ResourceProjectionService projectionService;

    public ResourceKafkaListener(ResourceEventParser parser, ResourceProjectionService projectionService) {
        this.parser = parser;
        this.projectionService = projectionService;
    }

    @KafkaListener(
            topics = TopicConfiguration.OFFICER_EVENTS,
            containerFactory = "resourceKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:resource-projection-service}")
    public void consumeOfficers(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        processEvent(record, acknowledgment);
    }

    @KafkaListener(
            topics = TopicConfiguration.VEHICLE_EVENTS,
            containerFactory = "resourceKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:resource-projection-service}")
    public void consumeVehicles(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        processEvent(record, acknowledgment);
    }

    @KafkaListener(
            topics = TopicConfiguration.UNIT_EVENTS,
            containerFactory = "resourceKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:resource-projection-service}")
    public void consumeUnits(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        processEvent(record, acknowledgment);
    }

    @KafkaListener(
            topics = TopicConfiguration.PERSON_EVENTS,
            containerFactory = "resourceKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:resource-projection-service}")
    public void consumePersons(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        processEvent(record, acknowledgment);
    }

    @KafkaListener(
            topics = TopicConfiguration.LOCATION_EVENTS,
            containerFactory = "resourceKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:resource-projection-service}")
    public void consumeLocations(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        processEvent(record, acknowledgment);
    }

    private void processEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            Object event = parser.parse(record.value(), null);
            projectionService.handle(event);
            log.debug("Parsed resource event from topic {}: {}", record.topic(), event.getClass().getSimpleName());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process Kafka resource event from topic {}", record.topic(), e);
            // acknowledge to avoid infinite retries; relies on idempotency
            acknowledgment.acknowledge();
        }
    }
}
