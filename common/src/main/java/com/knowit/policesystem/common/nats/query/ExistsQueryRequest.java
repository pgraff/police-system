package com.knowit.policesystem.common.nats.query;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Query request to check if a resource exists in a projection.
 */
@JsonTypeName("exists")
public class ExistsQueryRequest extends QueryRequest {
    
    private String resourceId;

    public ExistsQueryRequest() {
        super();
    }

    public ExistsQueryRequest(String queryId, String domain, String resourceId) {
        super(queryId, domain, "exists");
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
}

