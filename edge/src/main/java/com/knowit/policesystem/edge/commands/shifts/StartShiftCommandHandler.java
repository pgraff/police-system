package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.shifts.StartShiftRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.domain.ShiftStatus;
import com.knowit.policesystem.edge.dto.ShiftResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for StartShiftCommand.
 * Creates and publishes a StartShiftRequested event to Kafka.
 */
@Component
public class StartShiftCommandHandler implements CommandHandler<StartShiftCommand, ShiftResponseDto> {

    private static final String TOPIC = "shift-events";

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new start shift command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public StartShiftCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
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
    public ShiftResponseDto handle(StartShiftCommand command) {
        String statusString = command.getStatus() != null ? convertStatusToString(command.getStatus()) : null;

        StartShiftRequested event = new StartShiftRequested(
                command.getShiftId(),
                command.getStartTime(),
                command.getEndTime(),
                command.getShiftType() != null ? command.getShiftType().name() : null,
                statusString
        );

        eventPublisher.publish(TOPIC, command.getShiftId(), event);

        return new ShiftResponseDto(command.getShiftId());
    }

    private String convertStatusToString(ShiftStatus status) {
        if (status == ShiftStatus.InProgress) {
            return "In-Progress";
        }
        return status.name();
    }

    @Override
    public Class<StartShiftCommand> getCommandType() {
        return StartShiftCommand.class;
    }
}
