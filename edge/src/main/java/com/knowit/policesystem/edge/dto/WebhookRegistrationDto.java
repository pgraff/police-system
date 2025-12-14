package com.knowit.policesystem.edge.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Request DTO for registering a webhook subscription.
 */
public class WebhookRegistrationDto {

    @NotNull(message = "url is required")
    @Pattern(regexp = "^https?://.+", message = "url must be a valid HTTP/HTTPS URL")
    private String url;

    @NotEmpty(message = "events list cannot be empty")
    private List<String> events;

    private String secret;

    /**
     * Default constructor for Jackson deserialization.
     */
    public WebhookRegistrationDto() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
