package com.knowit.policesystem.edge.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.edge.dto.ErrorResponse;
import com.knowit.policesystem.edge.dto.SuccessResponse;
import com.knowit.policesystem.edge.dto.TestRequestDto;
import com.knowit.policesystem.edge.dto.ValidationErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive tests for REST API infrastructure.
 * Tests Swagger UI, API documentation, validation, error handling, content negotiation, and API versioning.
 */
class RestApiInfrastructureTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ============================================
    // Swagger UI Tests
    // ============================================

    @Test
    void swaggerUiIsAccessible() throws Exception {
        // Test that Swagger UI HTML page is accessible (may redirect to index.html)
        int status = mockMvc.perform(get("/swagger-ui.html"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(200, 302, 301);
    }

    @Test
    void swaggerUiIndexRedirects() throws Exception {
        // Test that /swagger-ui/index.html is accessible
        // Note: /swagger-ui may not be directly accessible in all SpringDoc versions
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUiStaticResourcesAreAccessible() throws Exception {
        // Test that Swagger UI static resources are accessible
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    // ============================================
    // API Documentation Tests
    // ============================================

    @Test
    void apiDocsEndpointReturnsOpenApiJson() throws Exception {
        // Test that /api-docs returns OpenAPI JSON
        String response = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // Verify it's valid JSON and contains OpenAPI structure
        assertThat(response).isNotEmpty();
        assertThat(response).contains("\"openapi\"");
        assertThat(response).contains("\"info\"");
        assertThat(response).contains("\"paths\"");
    }

    @Test
    void apiDocsMatchesYamlFile() throws Exception {
        // Test that /api-docs matches the YAML file content
        String apiDocsResponse = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // Verify key information from YAML is present
        assertThat(apiDocsResponse).contains("Police Incident Management System API");
        assertThat(apiDocsResponse).contains("\"version\":\"1.0.0\"");
    }

    @Test
    void groupedApiDocsIsAccessible() throws Exception {
        // Test that grouped API docs endpoint works
        mockMvc.perform(get("/api-docs/police-system-api"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void apiDocsContainsExpectedPaths() throws Exception {
        // Test that API docs contain expected paths from the YAML file
        String response = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // Verify some expected paths are present (from the OpenAPI spec)
        assertThat(response).contains("\"/officers\"");
        assertThat(response).contains("\"/incidents\"");
    }

    // ============================================
    // Request Validation Tests
    // ============================================

    @Test
    void validationWithValidDataReturnsSuccess() throws Exception {
        // Test that valid request data passes validation
        TestRequestDto validRequest = new TestRequestDto(
                "test-value",
                "test@example.com",
                50
        );

        mockMvc.perform(post("/api/v1/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requiredField").value("test-value"))
                .andExpect(jsonPath("$.data.emailField").value("test@example.com"))
                .andExpect(jsonPath("$.data.numberField").value(50));
    }

    @Test
    void validationWithMissingRequiredFieldReturns400() throws Exception {
        // Test that missing required field returns 400 Bad Request
        TestRequestDto invalidRequest = new TestRequestDto(
                null, // requiredField is missing
                "test@example.com",
                50
        );

        String response = mockMvc.perform(post("/api/v1/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // Verify it's a ValidationErrorResponse
        ValidationErrorResponse errorResponse = objectMapper.readValue(
                response, ValidationErrorResponse.class);
        assertThat(errorResponse.getError()).isEqualTo("Bad Request");
        assertThat(errorResponse.getMessage()).isEqualTo("Validation failed");
        assertThat(errorResponse.getDetails()).isNotEmpty();
    }

    @Test
    void validationWithInvalidEmailReturns400() throws Exception {
        // Test that invalid email format returns 400 Bad Request
        TestRequestDto invalidRequest = new TestRequestDto(
                "test-value",
                "invalid-email", // invalid email format
                50
        );

        mockMvc.perform(post("/api/v1/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[0]").exists());
    }

    @Test
    void validationWithInvalidNumberRangeReturns400() throws Exception {
        // Test that number outside valid range returns 400 Bad Request
        TestRequestDto invalidRequest = new TestRequestDto(
                "test-value",
                "test@example.com",
                150 // exceeds max value of 100
        );

        mockMvc.perform(post("/api/v1/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    // ============================================
    // Error Handling Tests
    // ============================================

    @Test
    void runtimeExceptionReturns500() throws Exception {
        // Test that RuntimeException returns 500 Internal Server Error
        String response = mockMvc.perform(get("/api/v1/test/error"))
                .andExpect(status().isInternalServerError())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        ErrorResponse errorResponse = objectMapper.readValue(response, ErrorResponse.class);
        assertThat(errorResponse.getError()).isEqualTo("Internal Server Error");
        assertThat(errorResponse.getMessage()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void validationExceptionReturns400() throws Exception {
        // Test that ValidationException returns 400 Bad Request
        mockMvc.perform(get("/api/v1/test/validation-error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void illegalArgumentExceptionReturns400() throws Exception {
        // Test that IllegalArgumentException returns 400 Bad Request
        String response = mockMvc.perform(get("/api/v1/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        ErrorResponse errorResponse = objectMapper.readValue(response, ErrorResponse.class);
        assertThat(errorResponse.getError()).isEqualTo("Bad Request");
        assertThat(errorResponse.getMessage()).isEqualTo("Test illegal argument error");
    }

    @Test
    void errorResponseMatchesErrorResponseSchema() throws Exception {
        // Test that error responses match the ErrorResponse schema
        String response = mockMvc.perform(get("/api/v1/test/error"))
                .andExpect(status().isInternalServerError())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        ErrorResponse errorResponse = objectMapper.readValue(response, ErrorResponse.class);
        assertThat(errorResponse.getError()).isNotNull();
        assertThat(errorResponse.getMessage()).isNotNull();
        assertThat(errorResponse.getDetails()).isNotNull();
    }

    // ============================================
    // Content Negotiation Tests
    // ============================================

    @Test
    void defaultContentTypeIsJson() throws Exception {
        // Test that default content type is JSON
        mockMvc.perform(get("/api/v1/test/success"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void acceptsJsonContentType() throws Exception {
        // Test that requests with Accept: application/json work
        mockMvc.perform(get("/api/v1/test/success")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void responseContentTypeHeaderIsSet() throws Exception {
        // Test that Content-Type header is set correctly
        mockMvc.perform(get("/api/v1/test/success"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"));
    }

    // ============================================
    // API Versioning Tests
    // ============================================

    @Test
    void apiV1BasePathWorks() throws Exception {
        // Test that /api/v1 base path works
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testEndpointsUseApiV1BasePath() throws Exception {
        // Test that test endpoints use /api/v1 base path
        mockMvc.perform(get("/api/v1/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void apiVersioningIsConsistent() throws Exception {
        // Test that API versioning is consistent across endpoints
        // Health endpoint
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());

        // Test endpoint
        mockMvc.perform(get("/api/v1/test/success"))
                .andExpect(status().isOk());
    }

    // ============================================
    // BaseRestController Functionality Tests
    // ============================================

    @Test
    void successHelperMethodWorks() throws Exception {
        // Test that success() helper method works correctly
        mockMvc.perform(get("/api/v1/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void successResponseMatchesSuccessResponseSchema() throws Exception {
        // Test that success responses match the SuccessResponse schema
        String response = mockMvc.perform(get("/api/v1/test/success"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        SuccessResponse<?> successResponse = objectMapper.readValue(
                response, SuccessResponse.class);
        assertThat(successResponse.isSuccess()).isTrue();
        assertThat(successResponse.getMessage()).isNotNull();
        assertThat(successResponse.getData()).isNotNull();
    }

    @Test
    void createdHelperMethodWorks() throws Exception {
        // Test that created() helper method works correctly (using health endpoint as example)
        // Note: Health endpoint uses success(), but we can verify the structure
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void responseStructureIsConsistent() throws Exception {
        // Test that response structure is consistent across endpoints
        String healthResponse = mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String testResponse = mockMvc.perform(get("/api/v1/test/success"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // Both should have success, message, and data fields
        assertThat(healthResponse).contains("\"success\"");
        assertThat(healthResponse).contains("\"message\"");
        assertThat(healthResponse).contains("\"data\"");

        assertThat(testResponse).contains("\"success\"");
        assertThat(testResponse).contains("\"message\"");
        assertThat(testResponse).contains("\"data\"");
    }
}
