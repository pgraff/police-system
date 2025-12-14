package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Metadata for API responses.
 * Provides correlation ID, timestamp, version, and HATEOAS links.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseMetadata {

    private String correlationId;
    private Instant timestamp;
    private String version;
    private Map<String, String> links;

    /**
     * Default constructor for Jackson serialization.
     */
    public ResponseMetadata() {
        this.timestamp = Instant.now();
        this.version = "1.0";
    }

    /**
     * Creates a new response metadata.
     *
     * @param correlationId the correlation ID for tracing
     * @param timestamp the response timestamp
     * @param version the API version
     * @param links HATEOAS-style links
     */
    public ResponseMetadata(String correlationId, Instant timestamp, String version, Map<String, String> links) {
        this.correlationId = correlationId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.version = version != null ? version : "1.0";
        this.links = links;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }
}
