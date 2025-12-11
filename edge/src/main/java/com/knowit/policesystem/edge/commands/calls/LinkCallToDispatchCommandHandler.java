package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.calls.LinkCallToDispatchRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.LinkCallToDispatchResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for LinkCallToDispatchCommand.
 * Creates and publishes a LinkCallToDispatchRequested event to Kafka.
 */
@Component
public class LinkCallToDispatchCommandHandler implements CommandHandler<LinkCallToDispatchCommand, LinkCallToDispatchResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new link call to dispatch command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public LinkCallToDispatchCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public LinkCallToDispatchResponseDto handle(LinkCallToDispatchCommand command) {
        // Create event from command
        LinkCallToDispatchRequested event = new LinkCallToDispatchRequested(
                command.getCallId(),
                command.getCallId(),
                command.getDispatchId()
        );

        // Publish event to Kafka topic
        // Note: This is NOT a critical event, so it only goes to Kafka (not NATS/JetStream)
        eventPublisher.publish(topicConfiguration.CALL_EVENTS, command.getCallId(), event);

        // Return response DTO
        return new LinkCallToDispatchResponseDto(command.getCallId(), command.getDispatchId());
    }

    @Override
    public Class<LinkCallToDispatchCommand> getCommandType() {
        return LinkCallToDispatchCommand.class;
    }
}
