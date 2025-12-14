package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.ResponseMetadata;
import com.knowit.policesystem.edge.dto.SuccessResponse;
import com.knowit.policesystem.edge.exceptions.ValidationException;
import com.knowit.policesystem.edge.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Base REST controller for all API endpoints.
 * Provides common functionality and response helpers.
 * All REST controllers should extend this class.
 */
public abstract class BaseRestController {

    /**
     * Creates a success response with 200 OK status.
     *
     * @param <T> the type of the response data
     * @param data the response data
     * @param message the success message
     * @return ResponseEntity with SuccessResponse
     */
    protected <T> ResponseEntity<SuccessResponse<T>> success(T data, String message) {
        SuccessResponse<T> response = new SuccessResponse<>(message, data);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a success response with 201 Created status.
     *
     * @param <T> the type of the response data
     * @param data the response data
     * @param message the success message
     * @return ResponseEntity with SuccessResponse and 201 Created status
     */
    protected <T> ResponseEntity<SuccessResponse<T>> created(T data, String message) {
        SuccessResponse<T> response = new SuccessResponse<>(message, data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates a success response with 200 OK status and default message.
     *
     * @param <T> the type of the response data
     * @param data the response data
     * @return ResponseEntity with SuccessResponse
     */
    protected <T> ResponseEntity<SuccessResponse<T>> success(T data) {
        return success(data, "Request processed successfully");
    }

    /**
     * Executes a command using the standard pattern: validate, find handler, execute, return response.
     * This method abstracts the common boilerplate found in all controller methods.
     *
     * @param command the command to execute
     * @param validator the validator to use for command validation
     * @param commandHandlerRegistry the command handler registry
     * @param commandClass the command class (for type-safe handler lookup)
     * @param successMessage the success message to include in the response
     * @param httpStatus the HTTP status code to return (CREATED for POST, OK for PUT/PATCH)
     * @param <C> the command type
     * @param <R> the response type
     * @return ResponseEntity with SuccessResponse containing the handler result
     * @throws ValidationException if command validation fails
     */
    @SuppressWarnings("unchecked")
    protected <C extends Command, R> ResponseEntity<SuccessResponse<R>> executeCommand(
            C command,
            Validator<? super C> validator,
            CommandHandlerRegistry commandHandlerRegistry,
            Class<? extends C> commandClass,
            String successMessage,
            HttpStatus httpStatus) {

        // Validate command
        var validationResult = validator.validate(command);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }

        // Get handler and execute
        CommandHandler<C, R> handler = (CommandHandler<C, R>) commandHandlerRegistry.findHandler((Class<C>) commandClass);
        R response = handler.handle(command);

        // Create metadata with correlation ID from command
        ResponseMetadata metadata = new ResponseMetadata();
        metadata.setCorrelationId(command.getCommandId().toString());
        metadata.setTimestamp(command.getTimestamp());

        // Return response with appropriate HTTP status
        SuccessResponse<R> successResponse = new SuccessResponse<>(successMessage, response, metadata);
        return ResponseEntity.status(httpStatus).body(successResponse);
    }
}

