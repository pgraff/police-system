package com.knowit.policesystem.projection.consumer;

import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.projection.service.WorkforceProjectionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class WorkforceKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(WorkforceKafkaListener.class);

    private final WorkforceEventParser parser;
    private final WorkforceProjectionService projectionService;

    public WorkforceKafkaListener(WorkforceEventParser parser, WorkforceProjectionService projectionService) {
        this.parser = parser;
        this.projectionService = projectionService;
    }

    @KafkaListener(
            topics = TopicConfiguration.SHIFT_EVENTS,
            containerFactory = "workforceKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:workforce-projection-service}")
    public void consumeShifts(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        processEvent(record, acknowledgment);
    }

    @KafkaListener(
            topics = TopicConfiguration.OFFICER_SHIFT_EVENTS,
            containerFactory = "workforceKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:workforce-projection-service}")
    public void consumeOfficerShifts(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        processEvent(record, acknowledgment);
    }

    private void processEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            Object event = parser.parse(record.value(), null);
            projectionService.handle(event);
            log.debug("Parsed workforce event from topic {}: {}", record.topic(), event.getClass().getSimpleName());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process Kafka workforce event from topic {}", record.topic(), e);
            // acknowledge to avoid infinite retries; relies on idempotency
            acknowledgment.acknowledge();
        }
    }
}
