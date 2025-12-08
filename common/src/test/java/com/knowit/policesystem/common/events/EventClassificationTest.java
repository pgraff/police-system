package com.knowit.policesystem.common.events;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for EventClassification utility class.
 */
class EventClassificationTest {

    @Test
    void generateNatsSubject_ReportIncidentRequested_ReturnsCorrectSubject() {
        // Given
        TestEvent event = new TestEvent("incident-123", "ReportIncidentRequested");

        // When
        String subject = EventClassification.generateNatsSubject(event);

        // Then
        assertThat(subject).isEqualTo("commands.incident.report");
    }

    @Test
    void generateNatsSubject_RegisterOfficerRequested_ReturnsCorrectSubject() {
        // Given
        TestEvent event = new TestEvent("officer-123", "RegisterOfficerRequested");

        // When
        String subject = EventClassification.generateNatsSubject(event);

        // Then
        assertThat(subject).isEqualTo("commands.officer.register");
    }

    @Test
    void generateNatsSubject_ChangeOfficerStatusRequested_ReturnsCorrectSubject() {
        // Given
        TestEvent event = new TestEvent("officer-123", "ChangeOfficerStatusRequested");

        // When
        String subject = EventClassification.generateNatsSubject(event);

        // Then
        assertThat(subject).isEqualTo("commands.officer.change-status");
    }

    @Test
    void generateNatsSubject_CreateUnitRequested_ReturnsCorrectSubject() {
        // Given
        TestEvent event = new TestEvent("unit-123", "CreateUnitRequested");

        // When
        String subject = EventClassification.generateNatsSubject(event);

        // Then
        assertThat(subject).isEqualTo("commands.unit.create");
    }

    @Test
    void generateNatsSubject_NullEvent_ThrowsException() {
        // When/Then
        assertThatThrownBy(() -> EventClassification.generateNatsSubject(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event cannot be null");
    }

    @Test
    void isCritical_ReportIncidentRequested_ReturnsTrue() {
        // Given
        TestEvent event = new TestEvent("incident-123", "ReportIncidentRequested");

        // When
        boolean isCritical = EventClassification.isCritical(event);

        // Then
        assertThat(isCritical).isTrue();
    }

    @Test
    void isCritical_NonRequestedEvent_ReturnsFalse() {
        // Given
        TestEvent event = new TestEvent("incident-123", "IncidentReported");

        // When
        boolean isCritical = EventClassification.isCritical(event);

        // Then
        assertThat(isCritical).isFalse();
    }

    @Test
    void isCritical_NullEvent_ReturnsFalse() {
        // When
        boolean isCritical = EventClassification.isCritical(null);

        // Then
        assertThat(isCritical).isFalse();
    }

    /**
     * Test event implementation for testing EventClassification.
     */
    private static class TestEvent extends Event {
        private final String eventType;

        TestEvent(String aggregateId, String eventType) {
            super(aggregateId);
            this.eventType = eventType;
        }

        @Override
        public String getEventType() {
            return eventType;
        }
    }
}
