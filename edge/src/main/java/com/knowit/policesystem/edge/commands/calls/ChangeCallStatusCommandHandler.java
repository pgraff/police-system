package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.calls.ChangeCallStatusRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.CallResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ChangeCallStatusCommand.
 */
@Component
public class ChangeCallStatusCommandHandler implements CommandHandler<ChangeCallStatusCommand, CallResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    public ChangeCallStatusCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
    }

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public CallResponseDto handle(ChangeCallStatusCommand command) {
        ChangeCallStatusRequested event = new ChangeCallStatusRequested(
                command.getCallId(),
                command.getStatus()
        );

        eventPublisher.publish("call-events", command.getCallId(), event);

        return new CallResponseDto(command.getCallId(), null);
    }

    @Override
    public Class<ChangeCallStatusCommand> getCommandType() {
        return ChangeCallStatusCommand.class;
    }
}
