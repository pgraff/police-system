package com.knowit.policesystem.edge.commands;

import java.time.Instant;
import java.util.UUID;

/**
 * Test command class for testing command infrastructure.
 * This is a concrete implementation of Command for testing purposes.
 */
public class TestCommand extends Command {
    private String testData;
    private int testNumber;

    public TestCommand() {
        super();
    }

    public TestCommand(String aggregateId) {
        super(aggregateId);
    }

    public TestCommand(String aggregateId, String testData, int testNumber) {
        super(aggregateId);
        this.testData = testData;
        this.testNumber = testNumber;
    }

    // Constructor for deserialization
    public TestCommand(UUID commandId, Instant timestamp, String aggregateId, 
                      String testData, int testNumber) {
        super(commandId, timestamp, aggregateId);
        this.testData = testData;
        this.testNumber = testNumber;
    }

    @Override
    public String getCommandType() {
        return "TestCommand";
    }

    public String getTestData() {
        return testData;
    }

    public void setTestData(String testData) {
        this.testData = testData;
    }

    public int getTestNumber() {
        return testNumber;
    }

    public void setTestNumber(int testNumber) {
        this.testNumber = testNumber;
    }
}

