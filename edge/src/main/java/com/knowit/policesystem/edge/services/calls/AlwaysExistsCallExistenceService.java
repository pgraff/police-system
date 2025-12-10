package com.knowit.policesystem.edge.services.calls;

import org.springframework.stereotype.Component;

/**
 * Default implementation that assumes calls exist.
 * For production this should be replaced with a real lookup; tests override with a test-scoped bean.
 */
@Component
public class AlwaysExistsCallExistenceService implements CallExistenceService {

    @Override
    public boolean exists(String callId) {
        return true;
    }
}
