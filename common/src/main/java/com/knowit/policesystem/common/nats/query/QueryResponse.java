package com.knowit.policesystem.common.nats.query;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base class for query responses from projections via NATS.
 * Query responses contain the result of a synchronous query operation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class QueryResponse {
    
    private String queryId;
    private boolean success;
    private String errorMessage;

    protected QueryResponse() {
    }

    protected QueryResponse(String queryId, boolean success, String errorMessage) {
        this.queryId = queryId;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public static QueryResponse error(String queryId, String errorMessage) {
        return new QueryResponse(queryId, false, errorMessage) {};
    }
}

