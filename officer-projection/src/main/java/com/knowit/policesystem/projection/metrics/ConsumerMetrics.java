package com.knowit.policesystem.projection.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple metrics component for tracking consumer errors and processing stats.
 * Provides basic observability for projection consumers.
 */
@Component
public class ConsumerMetrics {

    private static final Logger log = LoggerFactory.getLogger(ConsumerMetrics.class);

    private final AtomicLong processedEvents = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong lastErrorTimestamp = new AtomicLong(0);

    /**
     * Record a successfully processed event.
     */
    public void recordSuccess() {
        processedEvents.incrementAndGet();
    }

    /**
     * Record a processing error.
     *
     * @param eventType the type of event that failed
     * @param error     the exception that occurred
     */
    public void recordError(String eventType, Exception error) {
        long count = errorCount.incrementAndGet();
        lastErrorTimestamp.set(System.currentTimeMillis());
        log.warn("Consumer error #{} for event type: {}", count, eventType, error);
    }

    /**
     * Get the total number of successfully processed events.
     *
     * @return the count of processed events
     */
    public long getProcessedEvents() {
        return processedEvents.get();
    }

    /**
     * Get the total number of errors encountered.
     *
     * @return the count of errors
     */
    public long getErrorCount() {
        return errorCount.get();
    }

    /**
     * Get the timestamp of the last error (milliseconds since epoch).
     *
     * @return the timestamp, or 0 if no errors have occurred
     */
    public long getLastErrorTimestamp() {
        return lastErrorTimestamp.get();
    }

    /**
     * Reset all metrics (useful for testing).
     */
    public void reset() {
        processedEvents.set(0);
        errorCount.set(0);
        lastErrorTimestamp.set(0);
    }
}

