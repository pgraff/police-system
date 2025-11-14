package com.knowit.policesystem.edge.queries;

import java.time.Instant;
import java.util.UUID;

/**
 * Test query class for testing query infrastructure.
 * This is a concrete implementation of Query for testing purposes.
 */
public class TestQuery extends Query {
    private String searchTerm;
    private int limit;

    public TestQuery() {
        super();
    }

    public TestQuery(String searchTerm, int limit) {
        super();
        this.searchTerm = searchTerm;
        this.limit = limit;
    }

    // Constructor for deserialization
    public TestQuery(UUID queryId, Instant timestamp, String searchTerm, int limit) {
        super(queryId, timestamp);
        this.searchTerm = searchTerm;
        this.limit = limit;
    }

    @Override
    public String getQueryType() {
        return "TestQuery";
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}

