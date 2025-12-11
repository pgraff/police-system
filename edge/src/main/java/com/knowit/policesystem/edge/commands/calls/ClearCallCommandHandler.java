package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.calls.ClearCallRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.CallResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ClearCallCommand.
 */
@Component
public class ClearCallCommandHandler implements CommandHandler<ClearCallCommand, CallResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    public ClearCallCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
        this.topicConfiguration = topicConfiguration;
    }

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public CallResponseDto handle(ClearCallCommand command) {
        ClearCallRequested event = new ClearCallRequested(
                command.getCallId(),
                command.getClearedTime()
        );

        eventPublisher.publish(topicConfiguration.CALL_EVENTS, command.getCallId(), event);

        return new CallResponseDto(command.getCallId(), null);
    }

    @Override
    public Class<ClearCallCommand> getCommandType() {
        return ClearCallCommand.class;
    }
}
