package com.knowit.policesystem.edge.services.calls;

/**
 * Abstraction to verify whether a call exists before processing commands.
 * In production this can be backed by an external system; tests override with a controllable implementation.
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
