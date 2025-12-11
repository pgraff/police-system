package com.knowit.policesystem.projection.nats;

import com.knowit.policesystem.projection.config.NatsProperties;
import com.knowit.policesystem.projection.consumer.OfficerEventParser;
import com.knowit.policesystem.projection.service.OfficerProjectionService;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Component
public class OfficerNatsListener {

    private static final Logger log = LoggerFactory.getLogger(OfficerNatsListener.class);

    private final NatsProperties properties;
    private final OfficerEventParser parser;
    private final OfficerProjectionService projectionService;

    private Connection connection;
    private Dispatcher dispatcher;
    private String subject;

    public OfficerNatsListener(NatsProperties properties,
                               OfficerEventParser parser,
                               OfficerProjectionService projectionService) {
        this.properties = properties;
        this.parser = parser;
        this.projectionService = projectionService;
    }

    @PostConstruct
    public void start() {
        if (!properties.isEnabled()) {
            log.info("NATS listener disabled");
            return;
        }
        try {
            connection = Nats.connect(properties.getUrl());
            dispatcher = connection.createDispatcher(message -> {
                try {
                    String payload = new String(message.getData(), StandardCharsets.UTF_8);
                    Object event = parser.parse(payload, message.getSubject());
                    projectionService.handle(event);
                } catch (Exception e) {
                    log.error("Failed to process NATS officer event", e);
                }
            });
            subject = properties.getSubjectPrefix().isBlank()
                    ? "commands.officer.>"
                    : properties.getSubjectPrefix() + ".officer.>";
            dispatcher.subscribe(subject);
            log.info("Subscribed to NATS subject {}", subject);
        } catch (Exception e) {
            log.error("Failed to start NATS listener", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (dispatcher != null && subject != null) {
                dispatcher.unsubscribe(subject);
            }
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            log.warn("Error closing NATS resources", e);
        }
    }
}
