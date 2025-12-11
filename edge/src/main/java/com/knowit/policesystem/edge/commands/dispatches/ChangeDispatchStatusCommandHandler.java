package com.knowit.policesystem.edge.commands.dispatches;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.dispatches.ChangeDispatchStatusRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.DispatchResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ChangeDispatchStatusCommand.
 * Creates and publishes a ChangeDispatchStatusRequested event to Kafka.
 */
@Component
public class ChangeDispatchStatusCommandHandler implements CommandHandler<ChangeDispatchStatusCommand, DispatchResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new change dispatch status command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public ChangeDispatchStatusCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public DispatchResponseDto handle(ChangeDispatchStatusCommand command) {
        // Create event from command
        ChangeDispatchStatusRequested event = new ChangeDispatchStatusRequested(
                command.getDispatchId(),
                EnumConverter.convertEnumToString(command.getStatus())
        );

        // Publish event to Kafka topic
        eventPublisher.publish(topicConfiguration.DISPATCH_EVENTS, command.getDispatchId(), event);

        // Return response DTO
        return new DispatchResponseDto(command.getDispatchId());
    }

    @Override
    public Class<ChangeDispatchStatusCommand> getCommandType() {
        return ChangeDispatchStatusCommand.class;
    }
}
