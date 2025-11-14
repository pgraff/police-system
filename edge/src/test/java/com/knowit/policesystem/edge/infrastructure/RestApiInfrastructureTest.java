package com.knowit.policesystem.edge.infrastructure;

import com.knowit.policesystem.edge.controllers.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for REST API infrastructure.
 * Verifies Swagger UI, OpenAPI docs, health endpoint, and error handling.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RestApiInfrastructureTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:latest")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealthController healthController;

    @Test
    void contextLoads() {
        // Verify Spring Boot context loads successfully
        assertThat(healthController).isNotNull();
    }

    @Test
    void swaggerUiIsAccessible() throws Exception {
        // Verify Swagger UI is accessible
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void openApiDocsEndpointReturnsSpec() throws Exception {
        // Verify OpenAPI docs endpoint returns JSON
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Police Incident Management System API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"));
    }

    @Test
    void healthEndpointWorks() throws Exception {
        // Verify health endpoint returns proper response
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("police-system-edge"));
    }

    @Test
    void errorHandlingReturnsProperFormat() throws Exception {
        // Test a non-existent endpoint to verify error handling
        // Note: Spring Boot may return 500 for unmapped endpoints depending on configuration
        mockMvc.perform(get("/api/v1/nonexistent"))
                .andExpect(status().is5xxServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void apiVersioningIsConfigured() throws Exception {
        // Verify that API versioning path /api/v1 is working
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }
}

