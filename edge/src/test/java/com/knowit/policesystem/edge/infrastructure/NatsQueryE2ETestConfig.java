package com.knowit.policesystem.edge.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.nats.NatsQueryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Test configuration for E2E NATS query tests.
 * Overrides NatsQueryClient to ensure it's always enabled for E2E tests.
 * This is necessary because BaseIntegrationTest disables NATS queries by default.
 */
@TestConfiguration
public class NatsQueryE2ETestConfig {

    private static final Logger log = LoggerFactory.getLogger(NatsQueryE2ETestConfig.class);

    @Value("${nats.url:nats://localhost:4222}")
    private String natsUrl;

    // Create a replacement bean that's always enabled
    // This will override the one from NatsQueryConfig
    // Note: Bean name must match exactly "natsQueryClient" to override
    @Bean
    @Primary
    public NatsQueryClient natsQueryClient(ObjectMapper objectMapper) {
        // Force enable NATS query client for E2E tests
        // This overrides the bean from NatsQueryConfig which may be disabled
        log.info("Creating enabled NatsQueryClient for E2E tests with URL: {}", natsUrl);
        NatsQueryClient client = new NatsQueryClient(natsUrl, objectMapper, 5000L, true);
        log.info("NatsQueryClient created and enabled for E2E tests");
        return client;
    }
}

