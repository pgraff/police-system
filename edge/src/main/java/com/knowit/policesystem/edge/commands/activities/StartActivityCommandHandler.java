package com.knowit.policesystem.edge.commands.activities;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.activities.StartActivityRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.domain.ActivityStatus;
import com.knowit.policesystem.edge.dto.ActivityResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for StartActivityCommand.
 * Creates and publishes a StartActivityRequested event to Kafka.
 */
@Component
public class StartActivityCommandHandler implements CommandHandler<StartActivityCommand, ActivityResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new start activity command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public StartActivityCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public ActivityResponseDto handle(StartActivityCommand command) {
        // Convert status enum to string, handling InProgress -> "In-Progress"
        String statusString = command.getStatus() != null ? convertStatusToString(command.getStatus()) : null;

        // Create event from command
        StartActivityRequested event = new StartActivityRequested(
                command.getActivityId(),
                command.getActivityTime(),
                command.getActivityType() != null ? command.getActivityType().name() : null,
                command.getDescription(),
                statusString
        );

        // Publish event to Kafka topic "activity-events"
        eventPublisher.publish("activity-events", command.getActivityId(), event);

        // Return response DTO
        return new ActivityResponseDto(command.getActivityId());
    }

    /**
     * Converts ActivityStatus enum to string format used in API/events.
     * InProgress -> "In-Progress" (with hyphen)
     * Other values remain as-is.
     *
     * @param status the activity status enum
     * @return the string representation
     */
    private String convertStatusToString(ActivityStatus status) {
        if (status == ActivityStatus.InProgress) {
            return "In-Progress";
        }
        return status.name();
    }

    @Override
    public Class<StartActivityCommand> getCommandType() {
        return StartActivityCommand.class;
    }
}
