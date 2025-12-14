package com.knowit.policesystem.edge.infrastructure;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that require Kafka, NATS, and PostgreSQL containers.
 * Containers are shared across all test classes that extend this base class,
 * significantly reducing test execution time by avoiding repeated container startup/shutdown.
 * 
 * Uses the Singleton Containers pattern: containers are started once in a static initializer
 * and reused across all test classes, stopping only after all tests complete.
 * 
 * Note: We do NOT use @Testcontainers and @Container annotations here, as they manage
 * container lifecycle per test class. Instead, we manually start containers in a static block.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    protected static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:latest")
    );

    protected static final NatsTestContainer nats = new NatsTestContainer();

    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("police_test")
            .withUsername("test")
            .withPassword("test");

    // Flag to ensure containers are started only once
    private static volatile boolean containersStarted = false;
    private static final Object START_LOCK = new Object();

    static {
        // Start containers once when the class is first loaded
        // They will be shared across all test classes that extend this base class
        synchronized (START_LOCK) {
            if (!containersStarted) {
                try {
                    kafka.start();
                    nats.start();
                    postgres.start();
                    containersStarted = true;
                    
                    // Register shutdown hook to stop containers when JVM exits
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            if (kafka.isRunning()) {
                                kafka.stop();
                            }
                            if (nats.isRunning()) {
                                nats.stop();
                            }
                            if (postgres.isRunning()) {
                                postgres.stop();
                            }
                        } catch (Exception e) {
                            // Ignore errors during shutdown
                        }
                    }));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to start test containers", e);
                }
            }
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Ensure containers are started before Spring tries to access them
        synchronized (START_LOCK) {
            if (!containersStarted) {
                try {
                    kafka.start();
                    nats.start();
                    postgres.start();
                    containersStarted = true;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to start test containers", e);
                }
            }
        }
        
        // Containers are now guaranteed to be started
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("nats.url", nats::getNatsUrl);
        registry.add("nats.enabled", () -> "true");
        
        // PostgreSQL configuration for edge service (webhooks and idempotency)
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Disable NATS queries in tests - we use in-memory services instead
        // EXCEPTION: If this is an E2E test (indicated by system property), enable NATS queries
        // NatsQueryE2ETestBase sets this property before Spring context initialization
        boolean isE2ETest = Boolean.parseBoolean(System.getProperty("nats.query.e2e.test.enabled", "false"));
        if (!isE2ETest) {
            registry.add("nats.query.enabled", () -> "false");
        }
        // If isE2ETest is true, we don't set nats.query.enabled here,
        // allowing NatsQueryE2ETestBase to set it to true
    }
}
