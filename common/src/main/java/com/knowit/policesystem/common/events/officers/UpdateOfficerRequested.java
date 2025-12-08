package com.knowit.policesystem.common.events.officers;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to update an officer.
 * This event is published to Kafka and NATS/JetStream when an officer is updated via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 * All fields are nullable to support partial updates - null fields mean "don't update this field".
 */
public class UpdateOfficerRequested extends Event {

    private String badgeNumber;
    private String firstName;
    private String lastName;
    private String rank;
    private String email;
    private String phoneNumber;
    private String hireDate;

    /**
     * Default constructor for deserialization.
     */
    public UpdateOfficerRequested() {
        super();
    }

    /**
     * Creates a new UpdateOfficerRequested event.
     *
     * @param badgeNumber the badge number (used as aggregateId)
     * @param firstName the first name (nullable for partial updates)
     * @param lastName the last name (nullable for partial updates)
     * @param rank the rank (nullable for partial updates)
     * @param email the email address (nullable for partial updates)
     * @param phoneNumber the phone number (nullable for partial updates)
     * @param hireDate the hire date as string (ISO-8601 format: yyyy-MM-dd, nullable for partial updates)
     */
    public UpdateOfficerRequested(String badgeNumber, String firstName, String lastName,
                                  String rank, String email, String phoneNumber,
                                  String hireDate) {
        super(badgeNumber);
        this.badgeNumber = badgeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.rank = rank;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.hireDate = hireDate;
    }

    @Override
    public String getEventType() {
        return "UpdateOfficerRequested";
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getHireDate() {
        return hireDate;
    }

    public void setHireDate(String hireDate) {
        this.hireDate = hireDate;
    }
}
