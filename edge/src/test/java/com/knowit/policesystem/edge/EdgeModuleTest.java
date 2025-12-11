package com.knowit.policesystem.edge;

import com.knowit.policesystem.edge.infrastructure.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic test to verify the edge module compiles and Spring Boot context can load.
 * Also verifies that Kafka test containers can be started.
 */
class EdgeModuleTest extends BaseIntegrationTest {

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

