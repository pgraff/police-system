package com.knowit.policesystem.edge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for webhook functionality.
 * Enables async processing for webhook notifications.
 */
@Configuration
@EnableAsync
public class WebhookConfig {
    // Configuration for webhook retry, timeout, etc. can be added here
}
