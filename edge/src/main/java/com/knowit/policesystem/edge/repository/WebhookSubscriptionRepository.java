package com.knowit.policesystem.edge.repository;

import com.knowit.policesystem.edge.model.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for webhook subscriptions.
 */
@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, String> {

    /**
     * Finds all subscriptions that include the given event type.
     *
     * @param eventType the event type to find subscriptions for
     * @return list of subscriptions
     */
    @Query("SELECT s FROM WebhookSubscription s WHERE :eventType MEMBER OF s.events")
    List<WebhookSubscription> findByEventType(@Param("eventType") String eventType);
}
