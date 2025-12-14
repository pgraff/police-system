package com.knowit.policesystem.edge.repository;

import com.knowit.policesystem.edge.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for idempotency records.
 */
@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {

    /**
     * Finds an idempotency record by key and endpoint.
     *
     * @param idempotencyKey the idempotency key
     * @param endpoint the endpoint path
     * @return optional idempotency record
     */
    Optional<IdempotencyRecord> findByIdempotencyKeyAndEndpoint(String idempotencyKey, String endpoint);

    /**
     * Deletes expired idempotency records.
     *
     * @param now the current time
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM IdempotencyRecord r WHERE r.expiresAt < :now")
    int deleteExpiredRecords(@Param("now") Instant now);
}
