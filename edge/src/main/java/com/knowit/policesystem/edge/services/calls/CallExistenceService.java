package com.knowit.policesystem.edge.services.calls;

/**
 * Abstraction to verify whether a call exists before processing commands.
 * In production this is backed by projection queries via NATS; tests override with a controllable implementation.
 */
public interface CallExistenceService {

    /**
     * Checks whether the call exists.
     *
     * @param callId the call identifier
     * @return true if the call exists, false otherwise
     */
    boolean exists(String callId);
}
