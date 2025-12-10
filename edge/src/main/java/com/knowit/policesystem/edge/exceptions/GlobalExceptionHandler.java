package com.knowit.policesystem.edge.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.knowit.policesystem.edge.dto.ErrorResponse;
import com.knowit.policesystem.edge.dto.ValidationErrorResponse;
import com.knowit.policesystem.edge.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 * Handles all exceptions and converts them to appropriate HTTP responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ValidationException.
     *
     * @param exception the validation exception
     * @return 400 Bad Request with validation error response
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(ValidationException exception) {
        ValidationErrorResponse response = ValidationErrorResponse.fromValidationResult(
                exception.getValidationResult());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles MethodArgumentNotValidException from Spring Validation.
     *
     * @param exception the method argument validation exception
     * @return 400 Bad Request with validation error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        var fieldErrors = exception.getBindingResult().getFieldErrors();
        var details = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ValidationErrorResponse response = new ValidationErrorResponse(
                "Bad Request", "Validation failed", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles CommandHandlerNotFoundException.
     *
     * @param exception the command handler not found exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(CommandHandlerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommandHandlerNotFoundException(
            CommandHandlerNotFoundException exception) {
        ErrorResponse response = new ErrorResponse(
                "Internal Server Error", exception.getMessage(), List.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handles QueryHandlerNotFoundException.
     *
     * @param exception the query handler not found exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(QueryHandlerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQueryHandlerNotFoundException(
            QueryHandlerNotFoundException exception) {
        ErrorResponse response = new ErrorResponse(
                "Internal Server Error", exception.getMessage(), List.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handles NotFoundException.
     *
     * @param exception the not found exception
     * @return 404 Not Found with error response
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException exception) {
        ErrorResponse response = new ErrorResponse(
                "Not Found", exception.getMessage(), List.of());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles HttpMessageNotReadableException (e.g., invalid enum values, malformed JSON).
     *
     * @param exception the HTTP message not readable exception
     * @return 400 Bad Request
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {
        String message = "Invalid request body";
        if (exception.getCause() instanceof InvalidFormatException) {
            InvalidFormatException invalidFormat = (InvalidFormatException) exception.getCause();
            String fieldName = invalidFormat.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .reduce((first, second) -> second)
                    .orElse("unknown");
            String validValues = "unknown";
            if (invalidFormat.getTargetType().isEnum()) {
                @SuppressWarnings("unchecked")
                Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) invalidFormat.getTargetType();
                validValues = String.join(", ", java.util.Arrays.stream(enumClass.getEnumConstants())
                        .map(Enum::name)
                        .toArray(String[]::new));
            }
            message = String.format("Invalid value '%s' for field '%s'. Valid values are: %s",
                    invalidFormat.getValue(), fieldName, validValues);
        }
        ErrorResponse response = new ErrorResponse("Bad Request", message, List.of());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles IllegalArgumentException.
     *
     * @param exception the illegal argument exception
     * @return 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        ErrorResponse response = new ErrorResponse(
                "Bad Request", exception.getMessage(), List.of());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles all other exceptions.
     *
     * @param exception the exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {
        ErrorResponse response = new ErrorResponse(
                "Internal Server Error", "An unexpected error occurred", List.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

