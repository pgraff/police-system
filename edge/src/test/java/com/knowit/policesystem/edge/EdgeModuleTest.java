package com.knowit.policesystem.edge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.knowit.policesystem.edge.infrastructure.NatsTestContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic test to verify the edge module compiles and Spring Boot context can load.
 * Also verifies that Kafka test containers can be started.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class EdgeModuleTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:latest")
    );

    @Container
    static NatsTestContainer nats = new NatsTestContainer();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("nats.url", nats::getNatsUrl);
        registry.add("nats.enabled", () -> "true");
    }

    @Test
    void contextLoads() {
        // Basic test to verify Spring Boot context can load
        assertThat(true).isTrue();
    }

    @Test
    void kafkaTestContainerStarts() {
        // Verify that Kafka test container is running
        assertThat(kafka.isRunning()).isTrue();
        assertThat(kafka.getBootstrapServers()).isNotEmpty();
    }
}

