package com.knowit.policesystem.common.nats.query;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Query request to get a resource from a projection.
 */
@JsonTypeName("get")
public class GetQueryRequest extends QueryRequest {
    
    private String resourceId;

    public GetQueryRequest() {
        super();
    }

    public GetQueryRequest(String queryId, String domain, String resourceId) {
        super(queryId, domain, "get");
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
}

