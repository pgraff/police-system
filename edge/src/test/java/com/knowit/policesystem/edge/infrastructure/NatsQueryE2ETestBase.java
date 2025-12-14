package com.knowit.policesystem.edge.infrastructure;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for end-to-end NATS query integration tests.
 * Extends BaseIntegrationTest and adds a separate PostgreSQL container for projection services.
 * 
 * This base class:
 * - Uses BaseIntegrationTest's PostgreSQL container for edge service (webhooks/idempotency)
 * - Starts a separate PostgreSQL container for projection services
 * - Enables NATS queries (unlike BaseIntegrationTest which disables them)
 * - Provides shared infrastructure for E2E query tests
 */
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true"  // Allow overriding NatsQueryClient bean
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "nats.query.enabled=true"  // Force enable - this has higher precedence than @DynamicPropertySource
        }
)
@Import(NatsQueryE2ETestConfig.class)  // Import test config that provides enabled NatsQueryClient
public abstract class NatsQueryE2ETestBase extends BaseIntegrationTest {

    // Separate PostgreSQL container for projection services (different database)
    protected static final PostgreSQLContainer<?> projectionPostgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("police")
            .withUsername("test")
            .withPassword("test");

    private static volatile boolean projectionPostgresStarted = false;
    private static final Object PROJECTION_POSTGRES_START_LOCK = new Object();

    static {
        // Set system property to indicate this is an E2E test
        // This must be set BEFORE BaseIntegrationTest's @DynamicPropertySource runs
        // so that BaseIntegrationTest knows not to disable NATS queries
        System.setProperty("nats.query.e2e.test.enabled", "true");
        startProjectionPostgres();
    }

    private static void startProjectionPostgres() {
        if (projectionPostgresStarted) {
            return;
        }
        synchronized (PROJECTION_POSTGRES_START_LOCK) {
            if (projectionPostgresStarted) {
                return;
            }
            projectionPostgres.start();
            projectionPostgresStarted = true;
        }
    }

    @DynamicPropertySource
    static void configureE2EProperties(DynamicPropertyRegistry registry) {
        // Start projection PostgreSQL if not already started
        startProjectionPostgres();

        // Configure properties - containers are already started by BaseIntegrationTest
        // Note: BaseIntegrationTest already configures edge service datasource
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("nats.url", nats::getNatsUrl);
        registry.add("nats.enabled", () -> "true");
        
        // CRITICAL: Enable NATS queries for E2E tests
        // This MUST override BaseIntegrationTest's false setting
        // The issue is that @DynamicPropertySource methods might be called in any order,
        // so we need to ensure our setting takes precedence.
        // We'll use a supplier that always returns true, and set it as the last property
        // to ensure it overrides any previous setting
        System.setProperty("nats.query.enabled", "true");
        // Add this property LAST to ensure it overrides BaseIntegrationTest's setting
        // Use a direct value supplier that always returns true
        registry.add("nats.query.enabled", () -> {
            org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NatsQueryE2ETestBase.class);
            log.info("E2E Test: Setting nats.query.enabled to true (overriding BaseIntegrationTest)");
            return "true";
        });
        registry.add("nats.query.timeout", () -> "5000"); // 5 second timeout for E2E tests

        // Note: Edge service datasource is already configured by BaseIntegrationTest
        // The projectionPostgres container is for projection services, not the edge service
    }

    /**
     * Gets the PostgreSQL JDBC URL for use by projection services.
     */
    protected String getPostgresJdbcUrl() {
        return projectionPostgres.getJdbcUrl();
    }

    /**
     * Gets the PostgreSQL username for use by projection services.
     */
    protected String getPostgresUsername() {
        return projectionPostgres.getUsername();
    }

    /**
     * Gets the PostgreSQL password for use by projection services.
     */
    protected String getPostgresPassword() {
        return projectionPostgres.getPassword();
    }
}

