package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.shifts.RecordShiftChangeRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.ShiftChangeResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for RecordShiftChangeCommand.
 * Publishes RecordShiftChangeRequested events to Kafka.
 */
@Component
public class RecordShiftChangeCommandHandler implements CommandHandler<RecordShiftChangeCommand, ShiftChangeResponseDto> {

    private static final String TOPIC = "shift-events";

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    public RecordShiftChangeCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
    }

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public ShiftChangeResponseDto handle(RecordShiftChangeCommand command) {
        // Convert changeType enum to string
        String changeTypeString = command.getChangeType() != null ? command.getChangeType().name() : null;

        RecordShiftChangeRequested event = new RecordShiftChangeRequested(
                command.getShiftId(),
                command.getShiftChangeId(),
                command.getChangeTime(),
                changeTypeString,
                command.getNotes()
        );

        eventPublisher.publish(TOPIC, command.getShiftId(), event);

        return new ShiftChangeResponseDto(command.getShiftChangeId());
    }

    @Override
    public Class<RecordShiftChangeCommand> getCommandType() {
        return RecordShiftChangeCommand.class;
    }
}
