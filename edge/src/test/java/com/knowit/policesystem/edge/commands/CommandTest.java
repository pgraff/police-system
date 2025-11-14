package com.knowit.policesystem.edge.commands;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Command base class functionality including ID generation,
 * timestamp setting, aggregateId handling, and command type.
 */
class CommandTest {

    @Test
    void testCommand_WithDefaultConstructor_GeneratesIdAndTimestamp() {
        // Given & When
        TestCommand command = new TestCommand();

        // Then
        assertThat(command.getCommandId()).isNotNull();
        assertThat(command.getCommandId()).isInstanceOf(UUID.class);
        assertThat(command.getTimestamp()).isNotNull();
        assertThat(command.getTimestamp()).isInstanceOf(Instant.class);
        assertThat(command.getAggregateId()).isNull();
        assertThat(command.getCommandType()).isEqualTo("TestCommand");
    }

    @Test
    void testCommand_WithAggregateId_SetsAggregateId() {
        // Given
        String aggregateId = "test-aggregate-123";

        // When
        TestCommand command = new TestCommand(aggregateId);

        // Then
        assertThat(command.getCommandId()).isNotNull();
        assertThat(command.getTimestamp()).isNotNull();
        assertThat(command.getAggregateId()).isEqualTo(aggregateId);
        assertThat(command.getCommandType()).isEqualTo("TestCommand");
    }

    @Test
    void testCommand_WithFullConstructor_SetsAllFields() {
        // Given
        UUID commandId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        String aggregateId = "test-aggregate-456";
        String testData = "test data";
        int testNumber = 42;

        // When
        TestCommand command = new TestCommand(commandId, timestamp, aggregateId, testData, testNumber);

        // Then
        assertThat(command.getCommandId()).isEqualTo(commandId);
        assertThat(command.getTimestamp()).isEqualTo(timestamp);
        assertThat(command.getAggregateId()).isEqualTo(aggregateId);
        assertThat(command.getTestData()).isEqualTo(testData);
        assertThat(command.getTestNumber()).isEqualTo(testNumber);
        assertThat(command.getCommandType()).isEqualTo("TestCommand");
    }

    @Test
    void testCommand_TimestampIsSetOnCreation() {
        // Given
        Instant beforeCreation = Instant.now();

        // When
        TestCommand command = new TestCommand();
        Instant afterCreation = Instant.now();

        // Then
        assertThat(command.getTimestamp()).isAfterOrEqualTo(beforeCreation);
        assertThat(command.getTimestamp()).isBeforeOrEqualTo(afterCreation);
    }

    @Test
    void testCommand_EachCommandHasUniqueId() {
        // When
        TestCommand command1 = new TestCommand();
        TestCommand command2 = new TestCommand();

        // Then
        assertThat(command1.getCommandId()).isNotEqualTo(command2.getCommandId());
    }
}

