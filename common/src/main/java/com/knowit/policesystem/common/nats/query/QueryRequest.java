package com.knowit.policesystem.common.nats.query;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base class for query requests sent to projections via NATS.
 * Query requests are used to synchronously query projection state.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class QueryRequest {
    
    private String queryId;
    private String domain;
    private String operation;

    protected QueryRequest() {
    }

    protected QueryRequest(String queryId, String domain, String operation) {
        this.queryId = queryId;
        this.domain = domain;
        this.operation = operation;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}

