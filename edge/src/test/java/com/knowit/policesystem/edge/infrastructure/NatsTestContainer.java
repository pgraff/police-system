package com.knowit.policesystem.edge.infrastructure;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * NATS test container with JetStream enabled.
 * Provides a NATS server for integration tests.
 */
public class NatsTestContainer extends GenericContainer<NatsTestContainer> {

    private static final int NATS_PORT = 4222;
    private static final int MONITORING_PORT = 8222;
    private static final String NATS_IMAGE = "nats:2.10-alpine";

    public NatsTestContainer() {
        super(DockerImageName.parse(NATS_IMAGE));
        withCommand("-js", "-m", "8222");
        withExposedPorts(NATS_PORT, MONITORING_PORT);
        waitingFor(Wait.forHttp("/healthz").forPort(MONITORING_PORT).withStartupTimeout(Duration.ofSeconds(30)));
    }

    /**
     * Gets the NATS connection URL for client connections.
     * @return NATS URL in format nats://host:port
     */
    public String getNatsUrl() {
        return String.format("nats://%s:%d", getHost(), getMappedPort(NATS_PORT));
    }

    /**
     * Gets the monitoring URL for health checks.
     * @return HTTP URL for monitoring endpoint
     */
    public String getMonitoringUrl() {
        return String.format("http://%s:%d", getHost(), getMappedPort(MONITORING_PORT));
    }
}
