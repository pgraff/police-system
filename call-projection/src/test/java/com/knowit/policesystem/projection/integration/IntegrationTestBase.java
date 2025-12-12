package com.knowit.policesystem.projection.integration;

import com.knowit.policesystem.projection.support.NatsTestContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class that boots Spring with Kafka, NATS, and Postgres Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("police")
            .withUsername("test")
            .withPassword("test");

    protected static final NatsTestContainer nats = new NatsTestContainer();

    private static volatile boolean containersStarted = false;
    private static final Object START_LOCK = new Object();

    static {
        startContainers();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        startContainers();
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "call-projection-test-group");
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("projection.nats.enabled", () -> "true");
        registry.add("projection.nats.url", nats::getNatsUrl);
    }

    private static void startContainers() {
        if (containersStarted) {
            return;
        }
        synchronized (START_LOCK) {
            if (containersStarted) {
                return;
            }
            kafka.start();
            postgres.start();
            nats.start();
            containersStarted = true;
        }
    }
}

