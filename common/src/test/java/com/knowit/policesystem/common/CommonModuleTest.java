package com.knowit.policesystem.common;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic test to verify the common module compiles and can run tests.
 * Also verifies that Kafka test containers can be started.
 */
@Testcontainers
class CommonModuleTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:latest")
    );

    @Test
    void moduleCompilesAndRuns() {
        // Basic test to verify the module compiles and test framework works
        assertThat(true).isTrue();
    }

    @Test
    void kafkaTestContainerStarts() {
        // Verify that Kafka test container is running
        assertThat(kafka.isRunning()).isTrue();
        assertThat(kafka.getBootstrapServers()).isNotEmpty();
    }
}

