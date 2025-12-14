package com.knowit.policesystem.edge.services;

import com.knowit.policesystem.edge.model.IdempotencyRecord;
import com.knowit.policesystem.edge.repository.IdempotencyRecordRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;

/**
 * Service for managing idempotency records.
 * Handles storing and retrieving cached responses for idempotent requests.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private static final int DEFAULT_TTL_HOURS = 24;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * Finds a cached response for an idempotency key and endpoint.
     *
     * @param idempotencyKey the idempotency key
     * @param endpoint the endpoint path
     * @return optional cached response
     */
    public Optional<IdempotencyRecord> findCachedResponse(String idempotencyKey, String endpoint) {
        Optional<IdempotencyRecord> record = repository.findByIdempotencyKeyAndEndpoint(idempotencyKey, endpoint);
        if (record.isPresent() && record.get().getExpiresAt().isAfter(Instant.now())) {
            return record;
        }
        return Optional.empty();
    }

    /**
     * Stores a request/response pair for idempotency.
     *
     * @param idempotencyKey the idempotency key
     * @param endpoint the endpoint path
     * @param requestBody the request body (for hashing)
     * @param responseBody the response body
     * @param httpStatus the HTTP status code
     */
    @Transactional
    public void storeResponse(String idempotencyKey, String endpoint, String requestBody,
                               String responseBody, Integer httpStatus) {
        String requestHash = hashRequest(requestBody);
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey, endpoint, requestHash, responseBody, httpStatus, DEFAULT_TTL_HOURS
        );
        repository.save(record);
    }

    /**
     * Cleans up expired idempotency records.
     * Runs periodically via scheduled task.
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredRecords() {
        int deleted = repository.deleteExpiredRecords(Instant.now());
        if (deleted > 0) {
            // Log cleanup if needed
        }
    }

    private String hashRequest(String requestBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(requestBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
