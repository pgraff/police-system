package com.knowit.policesystem.projection.nats;

import com.knowit.policesystem.projection.config.NatsProperties;
import com.knowit.policesystem.projection.consumer.ResourceEventParser;
import com.knowit.policesystem.projection.service.ResourceProjectionService;
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
public class ResourceNatsListener {

    private static final Logger log = LoggerFactory.getLogger(ResourceNatsListener.class);

    private final NatsProperties properties;
    private final ResourceEventParser parser;
    private final ResourceProjectionService projectionService;

    private Connection connection;
    private Dispatcher dispatcher;
    private List<String> subjects;

    public ResourceNatsListener(NatsProperties properties, ResourceEventParser parser, ResourceProjectionService projectionService) {
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
            subjects.add(prefix + ".officer.>");
            subjects.add(prefix + ".vehicle.>");
            subjects.add(prefix + ".unit.>");
            subjects.add(prefix + ".person.>");
            subjects.add(prefix + ".location.>");
            
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
            log.debug("Parsed resource event from NATS subject {}: {}", message.getSubject(), event.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Failed to process NATS resource event from subject {}", message.getSubject(), e);
        }
    }

    @PreDestroy
    public void stop() {
        if (dispatcher != null) {
            dispatcher.unsubscribe("*");
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("Error closing NATS connection", e);
            }
        }
    }
}
