package com.knowit.policesystem.edge.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.edge.model.WebhookSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for notifying webhook subscribers of events.
 * Sends HTTP POST requests to registered webhook URLs asynchronously.
 */
@Service
public class WebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotifier.class);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    
    private final ObjectMapper objectMapper;

    public WebhookNotifier(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Notifies all subscribers of an event type.
     *
     * @param eventType the event type
     * @param eventData the event data to send
     * @param subscriptions the list of subscriptions to notify
     */
    @Async
    public void notifySubscribers(String eventType, Map<String, Object> eventData, List<WebhookSubscription> subscriptions) {
        for (WebhookSubscription subscription : subscriptions) {
            if (subscription.getEvents().contains(eventType)) {
                notifySubscription(subscription, eventData);
            }
        }
    }

    private void notifySubscription(WebhookSubscription subscription, Map<String, Object> eventData) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(subscription.getUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(serializeEventData(eventData)))
                    .build();

            CompletableFuture<HttpResponse<String>> responseFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            
            responseFuture.thenAccept(response -> {
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.debug("Webhook notification sent successfully to {}", subscription.getUrl());
                } else {
                    log.warn("Webhook notification failed for {}: status {}", subscription.getUrl(), response.statusCode());
                }
            }).exceptionally(ex -> {
                log.error("Error sending webhook to {}", subscription.getUrl(), ex);
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to send webhook to {}", subscription.getUrl(), e);
        }
    }

    private String serializeEventData(Map<String, Object> eventData) {
        try {
            return objectMapper.writeValueAsString(eventData);
        } catch (Exception e) {
            log.error("Failed to serialize event data", e);
            return "{}";
        }
    }
}
