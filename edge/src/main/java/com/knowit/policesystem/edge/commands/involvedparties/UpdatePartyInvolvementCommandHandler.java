package com.knowit.policesystem.edge.commands.involvedparties;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.involvedparty.UpdatePartyInvolvementRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.InvolvementResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for UpdatePartyInvolvementCommand.
 * Publishes UpdatePartyInvolvementRequested events to Kafka.
 */
@Component
public class UpdatePartyInvolvementCommandHandler implements CommandHandler<UpdatePartyInvolvementCommand, InvolvementResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new update party involvement command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public UpdatePartyInvolvementCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public InvolvementResponseDto handle(UpdatePartyInvolvementCommand command) {
        // Create event from command
        UpdatePartyInvolvementRequested event = new UpdatePartyInvolvementRequested(
                command.getInvolvementId(),
                command.getInvolvementId(),
                EnumConverter.convertEnumToString(command.getPartyRoleType()),
                command.getDescription()
        );

        // Publish event to Kafka topic
        eventPublisher.publish(topicConfiguration.INVOLVED_PARTY_EVENTS, command.getInvolvementId(), event);

        // Return response DTO
        return new InvolvementResponseDto(command.getInvolvementId());
    }

    @Override
    public Class<UpdatePartyInvolvementCommand> getCommandType() {
        return UpdatePartyInvolvementCommand.class;
    }
}
