package com.knowit.policesystem.common.nats.query;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Response to a get query request.
 * Contains the full resource data if found, or null if not found.
 * 
 * The data field is typed as Object to support different projection types.
 * Callers should deserialize to the appropriate type based on the domain.
 */
@JsonTypeName("get")
public class GetQueryResponse extends QueryResponse {
    
    private Object data;

    public GetQueryResponse() {
        super();
    }

    public GetQueryResponse(String queryId, Object data) {
        super(queryId, true, null);
        this.data = data;
    }

    public GetQueryResponse(String queryId, String errorMessage) {
        super(queryId, false, errorMessage);
        this.data = null;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

