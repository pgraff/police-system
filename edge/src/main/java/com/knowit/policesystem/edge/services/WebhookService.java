package com.knowit.policesystem.edge.services;

import com.knowit.policesystem.edge.dto.WebhookRegistrationDto;
import com.knowit.policesystem.edge.model.WebhookSubscription;
import com.knowit.policesystem.edge.repository.WebhookSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing webhook subscriptions.
 */
@Service
public class WebhookService {

    private final WebhookSubscriptionRepository repository;

    public WebhookService(WebhookSubscriptionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WebhookSubscription createSubscription(WebhookRegistrationDto dto) {
        WebhookSubscription subscription = new WebhookSubscription(
                dto.getUrl(),
                dto.getEvents(),
                dto.getSecret()
        );
        return repository.save(subscription);
    }

    @Transactional
    public void deleteSubscription(String id) {
        repository.deleteById(id);
    }

    public List<WebhookSubscription> getSubscriptions() {
        return repository.findAll();
    }

    public List<WebhookSubscription> getSubscriptionsForEvent(String eventType) {
        return repository.findByEventType(eventType);
    }
}
