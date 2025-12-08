package com.knowit.policesystem.common.events.persons;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to update a person.
 * This event is published to Kafka and NATS/JetStream when a person is updated via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 * All fields are nullable to support partial updates - null fields mean "don't update this field".
 */
public class UpdatePersonRequested extends Event {

    private String personId;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String gender;
    private String race;
    private String phoneNumber;

    /**
     * Default constructor for deserialization.
     */
    public UpdatePersonRequested() {
        super();
    }

    /**
     * Creates a new UpdatePersonRequested event.
     *
     * @param personId the person ID (used as aggregateId)
     * @param firstName the first name (nullable for partial updates)
     * @param lastName the last name (nullable for partial updates)
     * @param dateOfBirth the date of birth as string (ISO-8601 format: yyyy-MM-dd, nullable for partial updates)
     * @param gender the gender as string enum name (nullable for partial updates)
     * @param race the race as string enum name (nullable for partial updates)
     * @param phoneNumber the phone number (nullable for partial updates)
     */
    public UpdatePersonRequested(String personId, String firstName, String lastName,
                                 String dateOfBirth, String gender, String race, String phoneNumber) {
        super(personId);
        this.personId = personId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.race = race;
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String getEventType() {
        return "UpdatePersonRequested";
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
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

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
