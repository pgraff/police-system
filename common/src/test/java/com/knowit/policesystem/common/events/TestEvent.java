package com.knowit.policesystem.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Test event class for testing event infrastructure.
 * This is a concrete implementation of Event for testing purposes.
 */
public class TestEvent extends Event {
    private String testData;
    private int testNumber;

    public TestEvent() {
        super();
    }

    public TestEvent(String aggregateId, String testData, int testNumber) {
        super(aggregateId);
        this.testData = testData;
        this.testNumber = testNumber;
    }

    public TestEvent(String aggregateId, int version, String testData, int testNumber) {
        super(aggregateId, version);
        this.testData = testData;
        this.testNumber = testNumber;
    }

    // Constructor for deserialization
    public TestEvent(UUID eventId, Instant timestamp, String aggregateId, int version, 
                     String testData, int testNumber) {
        super(eventId, timestamp, aggregateId, version);
        this.testData = testData;
        this.testNumber = testNumber;
    }

    @Override
    public String getEventType() {
        return "TestEvent";
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

