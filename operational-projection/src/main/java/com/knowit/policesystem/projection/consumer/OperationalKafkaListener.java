package com.knowit.policesystem.projection.consumer;

import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.projection.service.OperationalProjectionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OperationalKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(OperationalKafkaListener.class);

    private final OperationalEventParser parser;
    private final OperationalProjectionService projectionService;

    public OperationalKafkaListener(OperationalEventParser parser, OperationalProjectionService projectionService) {
        this.parser = parser;
        this.projectionService = projectionService;
    }

    @KafkaListener(
            topics = TopicConfiguration.INCIDENT_EVENTS,
            containerFactory = "operationalKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:operational-projection-service}")
    public void consumeIncidents(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        processEvent(record, acknowledgment);
    }

    @KafkaListener(
            topics = TopicConfiguration.CALL_EVENTS,
            containerFactory = "operationalKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:operational-projection-service}")
    public void consumeCalls(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        processEvent(record, acknowledgment);
    }

    @KafkaListener(
            topics = TopicConfiguration.DISPATCH_EVENTS,
            containerFactory = "operationalKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:operational-projection-service}")
    public void consumeDispatches(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        processEvent(record, acknowledgment);
    }

    @KafkaListener(
            topics = TopicConfiguration.ACTIVITY_EVENTS,
            containerFactory = "operationalKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:operational-projection-service}")
    public void consumeActivities(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        processEvent(record, acknowledgment);
    }

    @KafkaListener(
            topics = TopicConfiguration.ASSIGNMENT_EVENTS,
            containerFactory = "operationalKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:operational-projection-service}")
    public void consumeAssignments(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        processEvent(record, acknowledgment);
    }

    @KafkaListener(
            topics = TopicConfiguration.INVOLVED_PARTY_EVENTS,
            containerFactory = "operationalKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:operational-projection-service}")
    public void consumeInvolvedParties(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        processEvent(record, acknowledgment);
    }

    @KafkaListener(
            topics = TopicConfiguration.RESOURCE_ASSIGNMENT_EVENTS,
            containerFactory = "operationalKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:operational-projection-service}")
    public void consumeResourceAssignments(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        processEvent(record, acknowledgment);
    }

    private void processEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            Object event = parser.parse(record.value(), null);
            projectionService.handle(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process Kafka operational event from topic {}", record.topic(), e);
            // acknowledge to avoid infinite retries; relies on idempotency
            acknowledgment.acknowledge();
        }
    }
}
