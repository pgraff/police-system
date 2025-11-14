package com.knowit.policesystem.edge.validation;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.TestCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for validation infrastructure including ValidationResult,
 * ValidationError, Validator interface, and CommandValidator base class.
 */
class ValidationFrameworkTest {

    @Test
    void testValidationResult_WithNoErrors_IsValid() {
        // When
        ValidationResult result = ValidationResult.valid();

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void testValidationResult_WithErrors_IsNotValid() {
        // Given
        ValidationError error1 = new ValidationError("field1", "Field is required", null);
        ValidationError error2 = new ValidationError("field2", "Invalid format", "invalid");

        // When
        ValidationResult result = ValidationResult.withErrors(List.of(error1, error2));

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors()).contains(error1, error2);
    }

    @Test
    void testValidationError_ContainsFieldAndMessage() {
        // When
        ValidationError error = new ValidationError("email", "Must be a valid email", "not-an-email");

        // Then
        assertThat(error.getField()).isEqualTo("email");
        assertThat(error.getMessage()).isEqualTo("Must be a valid email");
        assertThat(error.getRejectedValue()).isEqualTo("not-an-email");
    }

    @Test
    void testValidator_ValidateValidObject_ReturnsValidResult() {
        // Given
        Validator<String> validator = new TestValidator(true);

        // When
        ValidationResult result = validator.validate("valid");

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void testValidator_ValidateInvalidObject_ReturnsInvalidResult() {
        // Given
        Validator<String> validator = new TestValidator(false);

        // When
        ValidationResult result = validator.validate("invalid");

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getField()).isEqualTo("value");
    }

    @Test
    void testCommandValidator_CanValidateCommand() {
        // Given
        TestCommandValidator validator = new TestCommandValidator();
        TestCommand command = new TestCommand("aggregate-1", "test", 1);

        // When
        ValidationResult result = validator.validate(command);

        // Then
        assertThat(result).isNotNull();
        // The actual validation logic is in the test validator implementation
    }

    @Test
    void testCommandValidator_ValidateInvalidCommand_ReturnsErrors() {
        // Given
        TestCommandValidator validator = new TestCommandValidator();
        TestCommand command = new TestCommand("aggregate-1", "", -1); // Invalid: empty data, negative number

        // When
        ValidationResult result = validator.validate(command);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.hasErrors()).isTrue();
    }

    /**
     * Test validator implementation for testing Validator interface.
     */
    private static class TestValidator implements Validator<String> {
        private final boolean shouldBeValid;

        public TestValidator(boolean shouldBeValid) {
            this.shouldBeValid = shouldBeValid;
        }

        @Override
        public ValidationResult validate(String object) {
            if (shouldBeValid) {
                return ValidationResult.valid();
            } else {
                ValidationError error = new ValidationError("value", "Validation failed", object);
                return ValidationResult.withErrors(List.of(error));
            }
        }
    }

    /**
     * Test command validator implementation for testing CommandValidator.
     */
    private static class TestCommandValidator extends CommandValidator {
        @Override
        public ValidationResult validate(Command command) {
            if (command instanceof TestCommand) {
                TestCommand testCommand = (TestCommand) command;
                ValidationResult.Builder builder = ValidationResult.builder();

                if (testCommand.getTestData() == null || testCommand.getTestData().isEmpty()) {
                    builder.addError(new ValidationError("testData", "Test data is required", testCommand.getTestData()));
                }

                if (testCommand.getTestNumber() < 0) {
                    builder.addError(new ValidationError("testNumber", "Test number must be non-negative", testCommand.getTestNumber()));
                }

                return builder.build();
            }
            return ValidationResult.valid();
        }
    }
}

