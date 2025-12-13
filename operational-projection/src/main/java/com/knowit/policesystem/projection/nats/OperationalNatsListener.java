package com.knowit.policesystem.projection.nats;

import com.knowit.policesystem.projection.config.NatsProperties;
import com.knowit.policesystem.projection.consumer.OperationalEventParser;
import com.knowit.policesystem.projection.service.OperationalProjectionService;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.Nats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class OperationalNatsListener {

    private static final Logger log = LoggerFactory.getLogger(OperationalNatsListener.class);

    private final NatsProperties properties;
    private final OperationalEventParser parser;
    private final OperationalProjectionService projectionService;

    private Connection connection;
    private Dispatcher dispatcher;
    private List<String> subjects;

    public OperationalNatsListener(NatsProperties properties,
                                   OperationalEventParser parser,
                                   OperationalProjectionService projectionService) {
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
            dispatcher = connection.createDispatcher(this::handleMessage);
            
            String prefix = properties.getSubjectPrefix().isBlank() 
                    ? "commands" 
                    : properties.getSubjectPrefix() + ".commands";
            
            subjects = new ArrayList<>();
            subjects.add(prefix + ".incident.>");
            subjects.add(prefix + ".call.>");
            subjects.add(prefix + ".dispatch.>");
            subjects.add(prefix + ".activity.>");
            subjects.add(prefix + ".assignment.>");
            subjects.add(prefix + ".involved-party.>");
            subjects.add(prefix + ".resource-assignment.>");
            
            subjects.forEach(subject -> {
                dispatcher.subscribe(subject);
                log.info("Subscribed to NATS subject {}", subject);
            });
        } catch (Exception e) {
            log.error("Failed to start NATS listener", e);
        }
    }

    private void handleMessage(Message message) {
        try {
            String payload = new String(message.getData(), StandardCharsets.UTF_8);
            Object event = parser.parse(payload, message.getSubject());
            projectionService.handle(event);
        } catch (Exception e) {
            log.error("Failed to process NATS operational event from subject {}", message.getSubject(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (dispatcher != null && subjects != null) {
                subjects.forEach(subject -> {
                    try {
                        dispatcher.unsubscribe(subject);
                    } catch (Exception e) {
                        log.warn("Error unsubscribing from subject {}", subject, e);
                    }
                });
            }
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            log.warn("Error closing NATS resources", e);
        }
    }
}
