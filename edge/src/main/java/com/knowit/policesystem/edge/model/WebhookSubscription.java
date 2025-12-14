package com.knowit.policesystem.edge.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

/**
 * Entity representing a webhook subscription.
 * Stores webhook URLs and event types to notify.
 */
@Entity
@Table(name = "webhook_subscriptions")
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String url;

    @ElementCollection
    @CollectionTable(name = "webhook_subscription_events", joinColumns = @JoinColumn(name = "subscription_id"))
    @Column(name = "event_type")
    private List<String> events;

    @Column(name = "secret")
    private String secret;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Default constructor for JPA.
     */
    public WebhookSubscription() {
    }

    /**
     * Creates a new webhook subscription.
     *
     * @param url the webhook URL
     * @param events the list of event types to subscribe to
     * @param secret the secret for webhook signature
     */
    public WebhookSubscription(String url, List<String> events, String secret) {
        this.url = url;
        this.events = events;
        this.secret = secret;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public List<String> getEvents() { return events; }
    public void setEvents(List<String> events) { this.events = events; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
