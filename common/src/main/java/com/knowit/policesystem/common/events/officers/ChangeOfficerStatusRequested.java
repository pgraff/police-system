package com.knowit.policesystem.common.events.officers;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to change an officer's status.
 * This event is published to Kafka and NATS/JetStream when an officer's status is changed via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class ChangeOfficerStatusRequested extends Event {

    private String badgeNumber;
    private String status;

    /**
     * Default constructor for deserialization.
     */
    public ChangeOfficerStatusRequested() {
        super();
    }

    /**
     * Creates a new ChangeOfficerStatusRequested event.
     *
     * @param badgeNumber the badge number (used as aggregateId)
     * @param status the new status
     */
    public ChangeOfficerStatusRequested(String badgeNumber, String status) {
        super(badgeNumber);
        this.badgeNumber = badgeNumber;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "ChangeOfficerStatusRequested";
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
