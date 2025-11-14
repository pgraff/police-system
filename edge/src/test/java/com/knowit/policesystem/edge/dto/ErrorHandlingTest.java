package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for error handling DTOs including ErrorResponse, SuccessResponse,
 * and ValidationErrorResponse.
 */
class ErrorHandlingTest {

    @Test
    void testErrorResponse_ContainsErrorAndMessage() {
        // When
        ErrorResponse response = new ErrorResponse("Bad Request", "Validation failed", 
                List.of("field1 is required", "field2 must be valid email"));

        // Then
        assertThat(response.getError()).isEqualTo("Bad Request");
        assertThat(response.getMessage()).isEqualTo("Validation failed");
        assertThat(response.getDetails()).hasSize(2);
        assertThat(response.getDetails()).contains("field1 is required", "field2 must be valid email");
    }

    @Test
    void testErrorResponse_WithNoDetails_HasEmptyDetails() {
        // When
        ErrorResponse response = new ErrorResponse("Internal Server Error", "An error occurred", List.of());

        // Then
        assertThat(response.getError()).isEqualTo("Internal Server Error");
        assertThat(response.getMessage()).isEqualTo("An error occurred");
        assertThat(response.getDetails()).isEmpty();
    }

    @Test
    void testSuccessResponse_ContainsSuccessAndMessage() {
        // When
        SuccessResponse<String> response = new SuccessResponse<>("Operation completed", "result data");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Operation completed");
        assertThat(response.getData()).isEqualTo("result data");
    }

    @Test
    void testSuccessResponse_WithNullData_StillSuccess() {
        // When
        SuccessResponse<Object> response = new SuccessResponse<>("Operation completed", null);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Operation completed");
        assertThat(response.getData()).isNull();
    }

    @Test
    void testValidationErrorResponse_ContainsValidationErrors() {
        // Given
        ValidationError error1 = new ValidationError("field1", "Field is required", null);
        ValidationError error2 = new ValidationError("field2", "Invalid format", "invalid");
        ValidationResult validationResult = ValidationResult.withErrors(List.of(error1, error2));

        // When
        ValidationErrorResponse response = ValidationErrorResponse.fromValidationResult(validationResult);

        // Then
        assertThat(response.getError()).isEqualTo("Bad Request");
        assertThat(response.getMessage()).isEqualTo("Validation failed");
        assertThat(response.getDetails()).hasSize(2);
        assertThat(response.getDetails()).contains("field1: Field is required", "field2: Invalid format");
    }

    @Test
    void testValidationErrorResponse_FromValidResult_ReturnsNull() {
        // Given
        ValidationResult validResult = ValidationResult.valid();

        // When
        ValidationErrorResponse response = ValidationErrorResponse.fromValidationResult(validResult);

        // Then
        assertThat(response).isNull();
    }

    @Test
    void testErrorResponse_CanBeConvertedToJson() {
        // Given
        ErrorResponse response = new ErrorResponse("Bad Request", "Validation failed",
                List.of("field1 is required"));

        // When
        String error = response.getError();
        String message = response.getMessage();
        List<String> details = response.getDetails();

        // Then
        assertThat(error).isEqualTo("Bad Request");
        assertThat(message).isEqualTo("Validation failed");
        assertThat(details).containsExactly("field1 is required");
    }
}

