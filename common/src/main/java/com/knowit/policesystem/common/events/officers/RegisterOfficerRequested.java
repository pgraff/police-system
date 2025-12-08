package com.knowit.policesystem.common.events.officers;

import com.knowit.policesystem.common.events.Event;

import java.time.LocalDate;

/**
 * Event representing a request to register an officer.
 * This event is published to Kafka and NATS/JetStream when an officer is registered via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class RegisterOfficerRequested extends Event {

    private String badgeNumber;
    private String firstName;
    private String lastName;
    private String rank;
    private String email;
    private String phoneNumber;
    private String hireDate;
    private String status;

    /**
     * Default constructor for deserialization.
     */
    public RegisterOfficerRequested() {
        super();
    }

    /**
     * Creates a new RegisterOfficerRequested event.
     *
     * @param badgeNumber the badge number (used as aggregateId)
     * @param firstName the first name
     * @param lastName the last name
     * @param rank the rank
     * @param email the email address
     * @param phoneNumber the phone number
     * @param hireDate the hire date as string (ISO-8601 format: yyyy-MM-dd)
     * @param status the status as string
     */
    public RegisterOfficerRequested(String badgeNumber, String firstName, String lastName,
                                    String rank, String email, String phoneNumber,
                                    String hireDate, String status) {
        super(badgeNumber);
        this.badgeNumber = badgeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.rank = rank;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.hireDate = hireDate;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "RegisterOfficerRequested";
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
