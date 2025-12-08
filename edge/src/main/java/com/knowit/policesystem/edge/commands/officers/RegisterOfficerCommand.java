package com.knowit.policesystem.edge.commands.officers;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.OfficerStatus;
import com.knowit.policesystem.edge.dto.RegisterOfficerRequestDto;

import java.time.LocalDate;

/**
 * Command for registering an officer.
 * This command is processed by RegisterOfficerCommandHandler.
 */
public class RegisterOfficerCommand extends Command {

    private String badgeNumber;
    private String firstName;
    private String lastName;
    private String rank;
    private String email;
    private String phoneNumber;
    private LocalDate hireDate;
    private OfficerStatus status;

    /**
     * Default constructor for deserialization.
     */
    public RegisterOfficerCommand() {
        super();
    }

    /**
     * Creates a new register officer command from a DTO.
     *
     * @param aggregateId the aggregate identifier (badgeNumber)
     * @param dto the request DTO containing officer data
     */
    public RegisterOfficerCommand(String aggregateId, RegisterOfficerRequestDto dto) {
        super(aggregateId);
        this.badgeNumber = dto.getBadgeNumber();
        this.firstName = dto.getFirstName();
        this.lastName = dto.getLastName();
        this.rank = dto.getRank();
        this.email = dto.getEmail();
        this.phoneNumber = dto.getPhoneNumber();
        this.hireDate = dto.getHireDate();
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "RegisterOfficerCommand";
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

    public OfficerStatus getStatus() {
        return status;
    }

    public void setStatus(OfficerStatus status) {
        this.status = status;
    }
}
