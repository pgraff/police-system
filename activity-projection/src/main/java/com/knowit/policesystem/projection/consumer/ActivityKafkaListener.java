package com.knowit.policesystem.projection.consumer;

import com.knowit.policesystem.projection.service.ActivityProjectionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ActivityKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(ActivityKafkaListener.class);

    private final ActivityEventParser parser;
    private final ActivityProjectionService projectionService;

    public ActivityKafkaListener(ActivityEventParser parser, ActivityProjectionService projectionService) {
        this.parser = parser;
        this.projectionService = projectionService;
    }

    @KafkaListener(
            topics = "activity-events",
            containerFactory = "activityKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:projection-service}")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            Object event = parser.parse(record.value(), null);
            projectionService.handle(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process Kafka activity event", e);
            // acknowledge to avoid infinite retries; relies on idempotency
            acknowledgment.acknowledge();
        }
    }
}

