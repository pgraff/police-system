package com.knowit.policesystem.edge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.nats.NatsQueryClient;
import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for NATS query client and projection query service.
 * Enables synchronous querying of projections via NATS request-response.
 */
@Configuration
public class NatsQueryConfig {

    @Value("${nats.url:nats://localhost:4222}")
    private String natsUrl;

    @Value("${nats.enabled:true}")
    private boolean natsEnabled;

    @Value("${nats.query.timeout:2000}")
    private long queryTimeoutMillis;

    @Value("${nats.query.enabled:true}")
    private boolean queryEnabled;

    /**
     * Creates a NatsQueryClient bean for querying projections via NATS.
     *
     * @param objectMapper the ObjectMapper for JSON serialization
     * @return NatsQueryClient implementation
     */
    @Bean
    public NatsQueryClient natsQueryClient(ObjectMapper objectMapper) {
        boolean enabled = natsEnabled && queryEnabled;
        return new NatsQueryClient(natsUrl, objectMapper, queryTimeoutMillis, enabled);
    }

    /**
     * Creates a ProjectionQueryService bean for querying projections.
     *
     * @param queryClient the NATS query client
     * @return ProjectionQueryService implementation
     */
    @Bean
    public ProjectionQueryService projectionQueryService(NatsQueryClient queryClient) {
        return new ProjectionQueryService(queryClient);
    }
}

