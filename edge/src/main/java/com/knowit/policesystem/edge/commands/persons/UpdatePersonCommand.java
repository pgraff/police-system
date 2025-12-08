package com.knowit.policesystem.edge.commands.persons;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.Gender;
import com.knowit.policesystem.edge.domain.Race;
import com.knowit.policesystem.edge.dto.UpdatePersonRequestDto;

import java.time.LocalDate;

/**
 * Command for updating a person.
 * This command is processed by UpdatePersonCommandHandler.
 */
public class UpdatePersonCommand extends Command {

    private String personId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private Race race;
    private String phoneNumber;

    /**
     * Default constructor for deserialization.
     */
    public UpdatePersonCommand() {
        super();
    }

    /**
     * Creates a new update person command from a DTO.
     *
     * @param aggregateId the aggregate identifier (personId)
     * @param dto the request DTO containing person update data
     */
    public UpdatePersonCommand(String aggregateId, UpdatePersonRequestDto dto) {
        super(aggregateId);
        this.personId = aggregateId;
        this.firstName = dto.getFirstName();
        this.lastName = dto.getLastName();
        this.dateOfBirth = dto.getDateOfBirth();
        this.gender = dto.getGender();
        this.race = dto.getRace();
        this.phoneNumber = dto.getPhoneNumber();
    }

    @Override
    public String getCommandType() {
        return "UpdatePersonCommand";
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Race getRace() {
        return race;
    }

    public void setRace(Race race) {
        this.race = race;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
