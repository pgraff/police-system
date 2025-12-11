package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.calls.DispatchCallRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.CallResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for DispatchCallCommand.
 */
@Component
public class DispatchCallCommandHandler implements CommandHandler<DispatchCallCommand, CallResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    public DispatchCallCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
        this.topicConfiguration = topicConfiguration;
    }

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public CallResponseDto handle(DispatchCallCommand command) {
        DispatchCallRequested event = new DispatchCallRequested(
                command.getCallId(),
                command.getDispatchedTime()
        );

        eventPublisher.publish(topicConfiguration.CALL_EVENTS, command.getCallId(), event);

        return new CallResponseDto(command.getCallId(), null);
    }

    @Override
    public Class<DispatchCallCommand> getCommandType() {
        return DispatchCallCommand.class;
    }
}
