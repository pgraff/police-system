package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.shifts.ChangeShiftStatusRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.domain.ShiftStatus;
import com.knowit.policesystem.edge.dto.ShiftResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ChangeShiftStatusCommand.
 * Publishes ChangeShiftStatusRequested events to Kafka.
 */
@Component
public class ChangeShiftStatusCommandHandler implements CommandHandler<ChangeShiftStatusCommand, ShiftResponseDto> {

    private static final String TOPIC = "shift-events";

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    public ChangeShiftStatusCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
    }

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public ShiftResponseDto handle(ChangeShiftStatusCommand command) {
        // Convert status enum to string, handling InProgress -> "In-Progress"
        String statusString = command.getStatus() != null ? convertStatusToString(command.getStatus()) : null;

        ChangeShiftStatusRequested event = new ChangeShiftStatusRequested(
                command.getShiftId(),
                statusString
        );

        eventPublisher.publish(TOPIC, command.getShiftId(), event);

        return new ShiftResponseDto(command.getShiftId());
    }

    /**
     * Converts ShiftStatus enum to string format used in API/events.
     * InProgress -> "In-Progress" (with hyphen)
     * Other values remain as-is.
     *
     * @param status the shift status enum
     * @return the string representation
     */
    private String convertStatusToString(ShiftStatus status) {
        if (status == ShiftStatus.InProgress) {
            return "In-Progress";
        }
        return status.name();
    }

    @Override
    public Class<ChangeShiftStatusCommand> getCommandType() {
        return ChangeShiftStatusCommand.class;
    }
}
