package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.shifts.EndShiftRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.ShiftResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for EndShiftCommand.
 * Publishes EndShiftRequested events to Kafka.
 */
@Component
public class EndShiftCommandHandler implements CommandHandler<EndShiftCommand, ShiftResponseDto> {

    private static final String TOPIC = "shift-events";

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    public EndShiftCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
    }

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public ShiftResponseDto handle(EndShiftCommand command) {
        EndShiftRequested event = new EndShiftRequested(
                command.getShiftId(),
                command.getEndTime()
        );

        eventPublisher.publish(TOPIC, command.getShiftId(), event);

        return new ShiftResponseDto(command.getShiftId());
    }

    @Override
    public Class<EndShiftCommand> getCommandType() {
        return EndShiftCommand.class;
    }
}
