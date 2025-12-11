package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.calls.UpdateCallRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.CallResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for UpdateCallCommand.
 */
@Component
public class UpdateCallCommandHandler implements CommandHandler<UpdateCallCommand, CallResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    public UpdateCallCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
        this.topicConfiguration = topicConfiguration;
    }

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public CallResponseDto handle(UpdateCallCommand command) {
        UpdateCallRequested event = new UpdateCallRequested(
                command.getCallId(),
                EnumConverter.convertEnumToString(command.getPriority()),
                command.getDescription(),
                EnumConverter.convertEnumToString(command.getCallType())
        );

        eventPublisher.publish(topicConfiguration.CALL_EVENTS, command.getCallId(), event);

        return new CallResponseDto(command.getCallId(), null);
    }

    @Override
    public Class<UpdateCallCommand> getCommandType() {
        return UpdateCallCommand.class;
    }
}
