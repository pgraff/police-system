package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.calls.UpdateCallRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.CallResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for UpdateCallCommand.
 */
@Component
public class UpdateCallCommandHandler implements CommandHandler<UpdateCallCommand, CallResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    public UpdateCallCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
    }

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public CallResponseDto handle(UpdateCallCommand command) {
        UpdateCallRequested event = new UpdateCallRequested(
                command.getCallId(),
                command.getPriority() != null ? command.getPriority().name() : null,
                command.getDescription(),
                command.getCallType() != null ? command.getCallType().name() : null
        );

        eventPublisher.publish("call-events", command.getCallId(), event);

        return new CallResponseDto(command.getCallId(), null);
    }

    @Override
    public Class<UpdateCallCommand> getCommandType() {
        return UpdateCallCommand.class;
    }
}
