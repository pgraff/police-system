package com.knowit.policesystem.edge.commands.involvedparties;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.involvedparty.EndPartyInvolvementRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.InvolvementResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for EndPartyInvolvementCommand.
 * Publishes EndPartyInvolvementRequested events to Kafka.
 */
@Component
public class EndPartyInvolvementCommandHandler implements CommandHandler<EndPartyInvolvementCommand, InvolvementResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new end party involvement command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public EndPartyInvolvementCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public InvolvementResponseDto handle(EndPartyInvolvementCommand command) {
        // Create event from command
        EndPartyInvolvementRequested event = new EndPartyInvolvementRequested(
                command.getInvolvementId(),
                command.getInvolvementId(),
                command.getInvolvementEndTime()
        );

        // Publish event to Kafka topic
        eventPublisher.publish(topicConfiguration.INVOLVED_PARTY_EVENTS, command.getInvolvementId(), event);

        // Return response DTO
        return new InvolvementResponseDto(command.getInvolvementId());
    }

    @Override
    public Class<EndPartyInvolvementCommand> getCommandType() {
        return EndPartyInvolvementCommand.class;
    }
}
