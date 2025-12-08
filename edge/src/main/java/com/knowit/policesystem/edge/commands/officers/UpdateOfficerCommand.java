package com.knowit.policesystem.edge.commands.officers;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.UpdateOfficerRequestDto;

import java.time.LocalDate;

/**
 * Command for updating an officer.
 * This command is processed by UpdateOfficerCommandHandler.
 */
public class UpdateOfficerCommand extends Command {

    private String badgeNumber;
    private String firstName;
    private String lastName;
    private String rank;
    private String email;
    private String phoneNumber;
    private LocalDate hireDate;

    /**
     * Default constructor for deserialization.
     */
    public UpdateOfficerCommand() {
        super();
    }

    /**
     * Creates a new update officer command from a DTO.
     *
     * @param aggregateId the aggregate identifier (badgeNumber)
     * @param dto the request DTO containing officer update data
     */
    public UpdateOfficerCommand(String aggregateId, UpdateOfficerRequestDto dto) {
        super(aggregateId);
        this.badgeNumber = aggregateId;
        this.firstName = dto.getFirstName();
        this.lastName = dto.getLastName();
        this.rank = dto.getRank();
        this.email = dto.getEmail();
        this.phoneNumber = dto.getPhoneNumber();
        this.hireDate = dto.getHireDate();
    }

    @Override
    public String getCommandType() {
        return "UpdateOfficerCommand";
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

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }
}
