package com.knowit.policesystem.common.nats.query;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Response to an existence query request.
 */
@JsonTypeName("exists")
public class ExistsQueryResponse extends QueryResponse {
    
    private boolean exists;

    public ExistsQueryResponse() {
        super();
    }

    public ExistsQueryResponse(String queryId, boolean exists) {
        super(queryId, true, null);
        this.exists = exists;
    }

    public ExistsQueryResponse(String queryId, String errorMessage) {
        super(queryId, false, errorMessage);
        this.exists = false;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }
}

