package com.knowit.policesystem.edge.commands;

import com.knowit.policesystem.edge.exceptions.CommandHandlerNotFoundException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for command handlers.
 * Maintains a map of command types to their handlers.
 */
@Component
public class CommandHandlerRegistry {
    private final Map<Class<? extends Command>, CommandHandler<?, ?>> handlers = new HashMap<>();

    /**
     * Registers a command handler.
     *
     * @param handler the handler to register
     */
    public void register(CommandHandler<?, ?> handler) {
        handlers.put(handler.getCommandType(), handler);
    }

    /**
     * Finds a handler for the given command type.
     *
     * @param commandType the command class
     * @param <C> the command type
     * @param <R> the result type
     * @return the handler for the command type
     * @throws CommandHandlerNotFoundException if no handler is found
     */
    @SuppressWarnings("unchecked")
    public <C extends Command, R> CommandHandler<C, R> findHandler(Class<C> commandType) {
        CommandHandler<?, ?> handler = handlers.get(commandType);
        if (handler == null) {
            throw new CommandHandlerNotFoundException(commandType.getSimpleName());
        }
        return (CommandHandler<C, R>) handler;
    }
}

