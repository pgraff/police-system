package com.knowit.policesystem.edge.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity representing an idempotency record.
 * Stores request/response pairs keyed by idempotency key and endpoint.
 */
@Entity
@Table(name = "idempotency_records", indexes = {
    @Index(name = "idx_idempotency_key_endpoint", columnList = "idempotency_key,endpoint")
})
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Default constructor for JPA.
     */
    public IdempotencyRecord() {
    }

    /**
     * Creates a new idempotency record.
     *
     * @param idempotencyKey the idempotency key
     * @param endpoint the endpoint path
     * @param requestHash the hash of the request
     * @param response the cached response
     * @param httpStatus the HTTP status code
     * @param ttlHours time to live in hours (default 24)
     */
    public IdempotencyRecord(String idempotencyKey, String endpoint, String requestHash,
                             String response, Integer httpStatus, int ttlHours) {
        this.idempotencyKey = idempotencyKey;
        this.endpoint = endpoint;
        this.requestHash = requestHash;
        this.response = response;
        this.httpStatus = httpStatus;
        this.createdAt = Instant.now();
        this.expiresAt = Instant.now().plusSeconds(ttlHours * 3600L);
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
