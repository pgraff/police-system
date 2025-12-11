package com.knowit.policesystem.projection.consumer;

import com.knowit.policesystem.projection.service.OfficerProjectionService;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OfficerKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(OfficerKafkaListener.class);

    private final OfficerEventParser parser;
    private final OfficerProjectionService projectionService;

    public OfficerKafkaListener(OfficerEventParser parser, OfficerProjectionService projectionService) {
        this.parser = parser;
        this.projectionService = projectionService;
    }

    @KafkaListener(
            topics = TopicConfiguration.OFFICER_EVENTS,
            containerFactory = "officerKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:projection-service}")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            Object event = parser.parse(record.value(), null);
            projectionService.handle(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process Kafka officer event", e);
            // acknowledge to avoid infinite retries; relies on idempotency
            acknowledgment.acknowledge();
        }
    }
}
