package com.knowit.policesystem.projection.consumer;

import com.knowit.policesystem.projection.service.OperationalProjectionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OperationalKafkaListenerTest {

    @Mock
    private OperationalEventParser parser;

    @Mock
    private OperationalProjectionService projectionService;

    @Mock
    private Acknowledgment acknowledgment;

    private OperationalKafkaListener listener;

    @BeforeEach
    void setUp() {
        listener = new OperationalKafkaListener(parser, projectionService);
    }

    @Test
    void consumeIncidents_WithValidEvent_CallsParserAndService() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("incident-events", 0, 0L, "key", "payload");

        listener.consumeIncidents(record, acknowledgment);

        verify(parser).parse("payload", null);
        verify(projectionService).handle(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeCalls_WithValidEvent_CallsParserAndService() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("call-events", 0, 0L, "key", "payload");

        listener.consumeCalls(record, acknowledgment);

        verify(parser).parse("payload", null);
        verify(projectionService).handle(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeDispatches_WithValidEvent_CallsParserAndService() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("dispatch-events", 0, 0L, "key", "payload");

        listener.consumeDispatches(record, acknowledgment);

        verify(parser).parse("payload", null);
        verify(projectionService).handle(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeActivities_WithValidEvent_CallsParserAndService() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("activity-events", 0, 0L, "key", "payload");

        listener.consumeActivities(record, acknowledgment);

        verify(parser).parse("payload", null);
        verify(projectionService).handle(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeAssignments_WithValidEvent_CallsParserAndService() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("assignment-events", 0, 0L, "key", "payload");

        listener.consumeAssignments(record, acknowledgment);

        verify(parser).parse("payload", null);
        verify(projectionService).handle(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeInvolvedParties_WithValidEvent_CallsParserAndService() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("involved-party-events", 0, 0L, "key", "payload");

        listener.consumeInvolvedParties(record, acknowledgment);

        verify(parser).parse("payload", null);
        verify(projectionService).handle(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeResourceAssignments_WithValidEvent_CallsParserAndService() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("resource-assignment-events", 0, 0L, "key", "payload");

        listener.consumeResourceAssignments(record, acknowledgment);

        verify(parser).parse("payload", null);
        verify(projectionService).handle(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeIncidents_WithParserException_AcknowledgesAndLogsError() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("incident-events", 0, 0L, "key", "payload");
        org.mockito.Mockito.when(parser.parse("payload", null)).thenThrow(new RuntimeException("Parse error"));

        listener.consumeIncidents(record, acknowledgment);

        verify(parser).parse("payload", null);
        verify(projectionService, never()).handle(any());
        verify(acknowledgment).acknowledge(); // Should still acknowledge on error
    }
}
