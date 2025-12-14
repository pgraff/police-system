package com.knowit.policesystem.edge.controllers;

import com.knowit.policesystem.edge.dto.WebhookRegistrationDto;
import com.knowit.policesystem.edge.model.WebhookSubscription;
import com.knowit.policesystem.edge.services.WebhookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for webhook subscription management.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController extends BaseRestController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Registers a new webhook subscription.
     *
     * @param requestDto the webhook registration DTO
     * @return 201 Created with subscription ID
     */
    @PostMapping
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<WebhookSubscription>> registerWebhook(
            @Valid @RequestBody WebhookRegistrationDto requestDto) {

        WebhookSubscription subscription = webhookService.createSubscription(requestDto);
        return created(subscription, "Webhook subscription created");
    }

    /**
     * Lists all webhook subscriptions.
     *
     * @return 200 OK with list of subscriptions
     */
    @GetMapping
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<List<WebhookSubscription>>> listWebhooks() {
        List<WebhookSubscription> subscriptions = webhookService.getSubscriptions();
        return success(subscriptions, "Webhook subscriptions retrieved");
    }

    /**
     * Deletes a webhook subscription.
     *
     * @param id the subscription ID
     * @return 200 OK
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<com.knowit.policesystem.edge.dto.SuccessResponse<String>> deleteWebhook(@PathVariable String id) {
        webhookService.deleteSubscription(id);
        return success(id, "Webhook subscription deleted");
    }
}
