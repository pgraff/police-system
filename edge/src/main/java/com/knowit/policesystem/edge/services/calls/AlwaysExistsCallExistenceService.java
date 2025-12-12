package com.knowit.policesystem.edge.services.calls;

/**
 * Default implementation that assumes calls exist.
 * 
 * @deprecated Replaced by ProjectionCallExistenceService which queries projections via NATS.
 * This class is kept for backward compatibility but should not be used in production.
 * Tests should override with a test-scoped bean.
 */
@Deprecated
public class AlwaysExistsCallExistenceService implements CallExistenceService {

    @Override
    public boolean exists(String callId) {
        return true;
    }
}
