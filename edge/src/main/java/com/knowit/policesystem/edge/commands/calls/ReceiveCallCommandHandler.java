package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.CallResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ReceiveCallCommand.
 * Creates and publishes a ReceiveCallRequested event to Kafka.
 */
@Component
public class ReceiveCallCommandHandler implements CommandHandler<ReceiveCallCommand, CallResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new receive call command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public ReceiveCallCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
        this.topicConfiguration = topicConfiguration;
    }

    /**
     * Registers this handler in the command handler registry.
     * Called after dependency injection is complete.
     */
    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public CallResponseDto handle(ReceiveCallCommand command) {
        // Create event from command
        ReceiveCallRequested event = new ReceiveCallRequested(
                command.getCallId(),
                command.getCallNumber(),
                EnumConverter.convertEnumToString(command.getPriority()),
                EnumConverter.convertEnumToString(command.getStatus()),
                command.getReceivedTime(),
                command.getDescription(),
                EnumConverter.convertEnumToString(command.getCallType())
        );

        // Publish event to Kafka topic
        eventPublisher.publish(topicConfiguration.CALL_EVENTS, command.getCallId(), event);

        // Return response DTO
        return new CallResponseDto(command.getCallId(), command.getCallNumber());
    }

    @Override
    public Class<ReceiveCallCommand> getCommandType() {
        return ReceiveCallCommand.class;
    }
}
