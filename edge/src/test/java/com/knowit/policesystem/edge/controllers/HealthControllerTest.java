package com.knowit.policesystem.edge.controllers;

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
 * Tests for HealthController.
 * Verifies health endpoint functionality and response format.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class HealthControllerTest {

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
    void healthControllerIsLoaded() {
        // Verify HealthController is loaded by Spring
        assertThat(healthController).isNotNull();
    }

    @Test
    void healthEndpointReturns200Ok() throws Exception {
        // Verify health endpoint returns 200 OK
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpointReturnsJsonContentType() throws Exception {
        // Verify response content type is JSON
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void healthEndpointReturnsSuccessResponseStructure() throws Exception {
        // Verify response matches SuccessResponse structure
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void healthEndpointReturnsHealthData() throws Exception {
        // Verify health data contains expected fields
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("police-system-edge"));
    }

    @Test
    void healthEndpointResponseMatchesSuccessResponseType() throws Exception {
        // Verify the response structure matches SuccessResponse<Map<String, String>>
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(jsonPath("$.success").isBoolean())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data.status").isString())
                .andExpect(jsonPath("$.data.service").isString());
    }
}

