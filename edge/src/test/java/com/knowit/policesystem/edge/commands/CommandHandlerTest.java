package com.knowit.policesystem.edge.commands;

import com.knowit.policesystem.edge.exceptions.CommandHandlerNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for command handler infrastructure including handler registration,
 * lookup, execution, and exception handling.
 */
class CommandHandlerTest {

    private CommandHandlerRegistry registry;
    private TestCommandHandler testHandler;

    @BeforeEach
    void setUp() {
        registry = new CommandHandlerRegistry();
        testHandler = new TestCommandHandler();
    }

    @Test
    void testCommandHandler_CanHandleCommand() {
        // Given
        TestCommand command = new TestCommand("aggregate-1", "test data", 42);

        // When
        String result = testHandler.handle(command);

        // Then
        assertThat(result).isEqualTo("Processed: test data");
        assertThat(testHandler.getCommandType()).isEqualTo(TestCommand.class);
    }

    @Test
    void testCommandHandlerRegistry_RegisterHandler_StoresHandler() {
        // When
        registry.register(testHandler);

        // Then
        CommandHandler<TestCommand, String> found = registry.findHandler(TestCommand.class);
        assertThat(found).isNotNull();
        assertThat(found).isEqualTo(testHandler);
    }

    @Test
    void testCommandHandlerRegistry_FindHandler_ReturnsRegisteredHandler() {
        // Given
        registry.register(testHandler);
        TestCommand command = new TestCommand("aggregate-1", "test", 1);

        // When
        CommandHandler<TestCommand, String> handler = registry.findHandler(TestCommand.class);

        // Then
        assertThat(handler).isNotNull();
        String result = handler.handle(command);
        assertThat(result).isEqualTo("Processed: test");
    }

    @Test
    void testCommandHandlerRegistry_FindHandler_WhenNotRegistered_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> registry.findHandler(TestCommand.class))
                .isInstanceOf(CommandHandlerNotFoundException.class)
                .hasMessageContaining("TestCommand");
    }

    @Test
    void testCommandHandlerRegistry_RegisterMultipleHandlers_StoresAll() {
        // Given
        TestCommandHandler handler1 = new TestCommandHandler();
        AnotherTestCommandHandler handler2 = new AnotherTestCommandHandler();

        // When
        registry.register(handler1);
        registry.register(handler2);

        // Then
        assertThat(registry.findHandler(TestCommand.class)).isEqualTo(handler1);
        assertThat(registry.findHandler(AnotherTestCommand.class)).isEqualTo(handler2);
    }

    @Test
    void testCommandHandler_HandleCommand_ReturnsResult() {
        // Given
        TestCommand command = new TestCommand("aggregate-1", "handler test", 100);
        registry.register(testHandler);

        // When
        CommandHandler<TestCommand, String> handler = registry.findHandler(TestCommand.class);
        String result = handler.handle(command);

        // Then
        assertThat(result).isEqualTo("Processed: handler test");
    }

    /**
     * Test implementation of CommandHandler for TestCommand.
     */
    private static class TestCommandHandler implements CommandHandler<TestCommand, String> {
        @Override
        public String handle(TestCommand command) {
            return "Processed: " + command.getTestData();
        }

        @Override
        public Class<TestCommand> getCommandType() {
            return TestCommand.class;
        }
    }

    /**
     * Another test command for testing multiple handlers.
     */
    private static class AnotherTestCommand extends Command {
        private String data;

        public AnotherTestCommand() {
            super();
        }

        public AnotherTestCommand(String aggregateId, String data) {
            super(aggregateId);
            this.data = data;
        }

        @Override
        public String getCommandType() {
            return "AnotherTestCommand";
        }

        public String getData() {
            return data;
        }
    }

    /**
     * Test implementation of CommandHandler for AnotherTestCommand.
     */
    private static class AnotherTestCommandHandler implements CommandHandler<AnotherTestCommand, String> {
        @Override
        public String handle(AnotherTestCommand command) {
            return "Another: " + command.getData();
        }

        @Override
        public Class<AnotherTestCommand> getCommandType() {
            return AnotherTestCommand.class;
        }
    }
}

