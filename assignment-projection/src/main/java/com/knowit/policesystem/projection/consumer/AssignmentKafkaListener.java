package com.knowit.policesystem.projection.consumer;

import com.knowit.policesystem.projection.service.AssignmentProjectionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class AssignmentKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(AssignmentKafkaListener.class);

    private final AssignmentEventParser parser;
    private final AssignmentProjectionService projectionService;

    public AssignmentKafkaListener(AssignmentEventParser parser, AssignmentProjectionService projectionService) {
        this.parser = parser;
        this.projectionService = projectionService;
    }

    @KafkaListener(
            topics = "assignment-events",
            containerFactory = "assignmentKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:projection-service}")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            Object event = parser.parse(record.value(), null);
            projectionService.handle(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process Kafka assignment event", e);
            // acknowledge to avoid infinite retries; relies on idempotency
            acknowledgment.acknowledge();
        }
    }
}
