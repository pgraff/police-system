package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.calls.ArriveAtCallRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.CallResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ArriveAtCallCommand.
 */
@Component
public class ArriveAtCallCommandHandler implements CommandHandler<ArriveAtCallCommand, CallResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    public ArriveAtCallCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
        this.topicConfiguration = topicConfiguration;
    }

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public CallResponseDto handle(ArriveAtCallCommand command) {
        ArriveAtCallRequested event = new ArriveAtCallRequested(
                command.getCallId(),
                command.getArrivedTime()
        );

        eventPublisher.publish(topicConfiguration.CALL_EVENTS, command.getCallId(), event);

        return new CallResponseDto(command.getCallId(), null);
    }

    @Override
    public Class<ArriveAtCallCommand> getCommandType() {
        return ArriveAtCallCommand.class;
    }
}
