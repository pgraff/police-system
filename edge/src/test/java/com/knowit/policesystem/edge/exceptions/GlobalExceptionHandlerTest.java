package com.knowit.policesystem.edge.exceptions;

import com.knowit.policesystem.edge.dto.ErrorResponse;
import com.knowit.policesystem.edge.dto.ValidationErrorResponse;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for GlobalExceptionHandler including handling of ValidationException,
 * MethodArgumentNotValidException, CommandHandlerNotFoundException, and generic exceptions.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleValidationException_ReturnsBadRequest() {
        // Given
        ValidationError error1 = new ValidationError("field1", "Field is required", null);
        ValidationError error2 = new ValidationError("field2", "Invalid format", "invalid");
        ValidationResult validationResult = ValidationResult.withErrors(List.of(error1, error2));
        ValidationException exception = new ValidationException(validationResult);

        // When
        ResponseEntity<ValidationErrorResponse> response = exceptionHandler.handleValidationException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getDetails()).hasSize(2);
    }

    @Test
    void testHandleMethodArgumentNotValidException_ReturnsBadRequest() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        // Mock the binding result to return field errors
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        org.springframework.validation.FieldError fieldError = new org.springframework.validation.FieldError(
                "testObject", "field1", "Field is required");
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // When
        ResponseEntity<ValidationErrorResponse> response = exceptionHandler.handleMethodArgumentNotValidException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getDetails()).hasSize(1);
    }

    @Test
    void testHandleCommandHandlerNotFoundException_ReturnsInternalServerError() {
        // Given
        CommandHandlerNotFoundException exception = new CommandHandlerNotFoundException("TestCommand");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleCommandHandlerNotFoundException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).contains("TestCommand");
    }

    @Test
    void testHandleQueryHandlerNotFoundException_ReturnsInternalServerError() {
        // Given
        QueryHandlerNotFoundException exception = new QueryHandlerNotFoundException("TestQuery");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleQueryHandlerNotFoundException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).contains("TestQuery");
    }

    @Test
    void testHandleIllegalArgumentException_ReturnsBadRequest() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid argument");
    }

    @Test
    void testHandleGenericException_ReturnsInternalServerError() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }
}

