package com.knowit.policesystem.projection.consumer;

import com.knowit.policesystem.projection.metrics.ConsumerMetrics;
import com.knowit.policesystem.projection.service.OfficerProjectionService;
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
    private final ConsumerMetrics metrics;

    public OfficerKafkaListener(OfficerEventParser parser, OfficerProjectionService projectionService,
                                ConsumerMetrics metrics) {
        this.parser = parser;
        this.projectionService = projectionService;
        this.metrics = metrics;
    }

    @KafkaListener(
            topics = "officer-events",
            containerFactory = "officerKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:projection-service}")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            Object event = parser.parse(record.value(), null);
            projectionService.handle(event);
            metrics.recordSuccess();
            acknowledgment.acknowledge();
        } catch (Exception e) {
            String eventType = extractEventType(record.value());
            metrics.recordError(eventType, e);
            log.error("Failed to process Kafka officer event [topic={}, partition={}, offset={}]",
                    record.topic(), record.partition(), record.offset(), e);
            // acknowledge to avoid infinite retries; relies on idempotency
            acknowledgment.acknowledge();
        }
    }

    private String extractEventType(String json) {
        try {
            if (json != null && json.contains("\"eventType\"")) {
                int start = json.indexOf("\"eventType\"") + 12;
                int end = json.indexOf("\"", start);
                if (end > start) {
                    return json.substring(start, end);
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "unknown";
    }
}
